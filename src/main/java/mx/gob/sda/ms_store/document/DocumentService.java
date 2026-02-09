package mx.gob.sda.ms_store.document;

import io.minio.PutObjectArgs;
import io.minio.MinioClient;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final MinioClient minioClient;
    private final VaultTemplate vaultTemplate;
    private final DocumentRepository repository;
    
    @Value("${minio.bucket}")
    private String bucket;

    private void establecerContextoSeguro(String tenantId, String departmentId) {
        String t = (tenantId == null) ? "" : tenantId.trim();
        String d = (departmentId == null) ? "" : departmentId.trim();
        repository.setSessionContext(t, d);
    }

    @Transactional
    public CompletableFuture<Map<String, Object>> processUpload(MultipartFile file, String tenantId, String departmentId, String userId, String clientIp) {
        try {
            establecerContextoSeguro(tenantId, departmentId);
            UUID tenantUuid = UUID.fromString(tenantId.trim());
            UUID deptUuid = UUID.fromString(departmentId.trim());

            if (!repository.validateDepartmentInTenant(deptUuid, tenantUuid)) {
                throw new SecurityException("Acceso denegado: El departamento no pertenece al organismo.");
            }

            byte[] fileBytes = file.getBytes();
            String fileHash = bytesToHex(MessageDigest.getInstance("SHA-256").digest(fileBytes));
            String objectKey = fileHash + ".enc";
            byte[] dek = new byte[32];
            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(dek);
            new SecureRandom().nextBytes(iv);
            
            String ciphertextDEK = vaultTemplate.opsForTransit()
                .encrypt("sda-master-key", Base64.getEncoder().encodeToString(dek));
            DocumentEntity doc = DocumentEntity.builder()
                .tenantId(tenantUuid)
                .departmentId(deptUuid)
                .fileHash(fileHash)
                .fileSizeBytes(file.getSize())
                .s3ObjectKey(objectKey)
                .status("PENDING_STORAGE")
                .contentType(file.getContentType())
                .dekWrappedValue(ciphertextDEK)
                .initializationVector(Base64.getEncoder().encodeToString(iv))
                .createdBy(userId)
                .clientIp(clientIp)
                .build();

            doc = repository.save(doc);
            log.info("Registro creado en DB (ID: {}). Iniciando subida a MinIO...", doc.getDocumentId());

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dek, "AES"), new IvParameterSpec(iv));
            byte[] encryptedData = cipher.doFinal(fileBytes);

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(encryptedData), encryptedData.length, -1)
                    .contentType("application/octet-stream")
                    .build()
            );

            doc.setStatus("RECEIVED");
            repository.save(doc);
            log.info("Proceso completado exitosamente para el Hash: {}", fileHash);

            Map<String, Object> response = new HashMap<>();
            response.put("document_id", doc.getDocumentId());
            response.put("file_hash", fileHash);
            response.put("status", "RECEIVED");
            
            return CompletableFuture.completedFuture(response);

        } catch (Exception e) {
            log.error("Error en processUpload: {}", e.getMessage());
            return CompletableFuture.failedFuture(e);
        }
    }

    @Transactional(readOnly = true)
    public StreamingResponseBody downloadDocument(String fileHash, String tenantId, String departmentId) {
        establecerContextoSeguro(tenantId, departmentId);

        DocumentEntity doc = repository.findByFileHash(fileHash)
           .orElseThrow(() -> new NoSuchElementException("Documento no encontrado o sin permisos."));

        String plaintextDEK = vaultTemplate.opsForTransit()
            .decrypt("sda-master-key", doc.getDekWrappedValue());
    
        final byte[] dek = Base64.getDecoder().decode(plaintextDEK);
        final byte[] iv = Base64.getDecoder().decode(doc.getInitializationVector());

        return outputStream -> {
            try (InputStream encryptedStream = minioClient.getObject(
                GetObjectArgs.builder().bucket(bucket).object(doc.getS3ObjectKey()).build())) {
            
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dek, "AES"), new IvParameterSpec(iv));
            
                try (CipherInputStream cis = new CipherInputStream(encryptedStream, cipher)) {
                    cis.transferTo(outputStream);
                    outputStream.flush();
                }
            } catch (Exception e) {
                log.error("Error crítico durante el flujo de descarga: {}", e.getMessage());
                throw new RuntimeException("Error al procesar el archivo para descarga", e);
            }
        };
    }

    @Transactional(readOnly = true)
    public Optional<DocumentEntity> getMetadata(String fileHash, String tenantId, String departmentId) {
        establecerContextoSeguro(tenantId, departmentId);
        return repository.findByFileHash(fileHash);
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}