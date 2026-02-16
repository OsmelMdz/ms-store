package mx.gob.sda.ms_store.document;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.GetObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.vault.support.VaultResponse;
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
    private static final String VAULT_KV_PATH = "secret/data/documents/%s/%s";

    @Value("${minio.bucket}")
    private String bucket;

    private void establecerContextoSeguro(String tenantId, String departmentId) {
        repository.setSessionContext(tenantId, (departmentId == null) ? "" : departmentId);
    }

    @Transactional(readOnly = true)
    public Optional<DocumentEntity> getMetadata(String fileHash, String tenantId, String departmentId) {
        establecerContextoSeguro(tenantId, departmentId);
        return repository.findByFileHash(fileHash);
    }

    @Transactional
    public CompletableFuture<Map<String, Object>> processUpload(MultipartFile file, String tenantId, String departmentId, String userId, String clientIp) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                establecerContextoSeguro(tenantId, departmentId);
                
                UUID tenantUuid = UUID.fromString(tenantId);
                UUID deptUuid = UUID.fromString(departmentId);
                
                byte[] dek = new byte[32];
                byte[] iv = new byte[16];
                SecureRandom random = new SecureRandom();
                random.nextBytes(dek);
                random.nextBytes(iv);

                String ciphertextDEK = vaultTemplate.opsForTransit()
                        .encrypt("sda-master-key", Base64.getEncoder().encodeToString(dek));

                byte[] fileBytes = file.getBytes();
                String fileHash = bytesToHex(MessageDigest.getInstance("SHA-256").digest(fileBytes));
                UUID documentId = UUID.randomUUID();

                String vaultPath = String.format(VAULT_KV_PATH, tenantId, documentId);
                Map<String, String> secretData = new HashMap<>();
                secretData.put("dek_wrapped", ciphertextDEK);
                secretData.put("iv", Base64.getEncoder().encodeToString(iv));
                vaultTemplate.write(vaultPath, Collections.singletonMap("data", secretData));

                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(dek, "AES"), new IvParameterSpec(iv));
                byte[] encryptedData = cipher.doFinal(fileBytes);

                String objectKey = tenantId + "/" + documentId + ".enc";
                minioClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .stream(new ByteArrayInputStream(encryptedData), encryptedData.length, -1)
                        .contentType("application/octet-stream")
                        .build()
                );

                DocumentEntity doc = new DocumentEntity();
                doc.setDocumentId(documentId);
                doc.setTenantId(tenantUuid);
                doc.setDepartmentId(deptUuid);
                doc.setFileHash(fileHash);
                doc.setFileSizeBytes((long) encryptedData.length);
                doc.setS3ObjectKey(objectKey);
                doc.setContentType(file.getContentType());
                doc.setDekWrappedValue(ciphertextDEK);
                doc.setInitializationVector(Base64.getEncoder().encodeToString(iv));
                doc.setStatus("RECEIVED");
                doc.setCreatedBy(userId);
                doc.setClientIp(clientIp);
                
                repository.save(doc);

                return Map.of(
                    "document_id", documentId.toString(),
                    "status", "SUCCESS",
                    "hash", fileHash
                );

            } catch (Exception e) {
                log.error("Error crítico en upload: {}", e.getMessage());
                throw new RuntimeException("Fallo en el pipeline de seguridad: " + e.getMessage());
            }
        });
    }

    @Transactional(readOnly = true)
    public StreamingResponseBody downloadDocument(String fileHash, String tenantId, String departmentId) {
        establecerContextoSeguro(tenantId, departmentId);

        DocumentEntity doc = repository.findByFileHash(fileHash)
            .orElseThrow(() -> new NoSuchElementException("Documento no encontrado o acceso denegado."));

        String vaultPath = String.format(VAULT_KV_PATH, tenantId, doc.getDocumentId());
        VaultResponse vRes = vaultTemplate.read(vaultPath);
        Map<String, Object> data = (Map<String, Object>) vRes.getData().get("data");

        String plaintextDEK = vaultTemplate.opsForTransit()
                .decrypt("sda-master-key", (String) data.get("dek_wrapped"));
        
        byte[] dek = Base64.getDecoder().decode(plaintextDEK);
        byte[] iv = Base64.getDecoder().decode((String) data.get("iv"));

        return outputStream -> {
            try (InputStream s3Stream = minioClient.getObject(
                    GetObjectArgs.builder().bucket(bucket).object(doc.getS3ObjectKey()).build())) {
                
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(dek, "AES"), new IvParameterSpec(iv));
                
                try (CipherInputStream cis = new CipherInputStream(s3Stream, cipher)) {
                    cis.transferTo(outputStream);
                    outputStream.flush();
                }
            } catch (Exception e) {
                throw new RuntimeException("Error en streaming de descarga", e);
            }
        };
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}