package mx.gob.sda.ms_store.document;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.Map;
import java.util.UUID;
import java.util.NoSuchElementException;
import java.util.concurrent.CompletableFuture;

@Slf4j
@RestController
@RequestMapping("/api/v1/{tenant_id}/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public CompletableFuture<ResponseEntity<?>> upload(
            @PathVariable("tenant_id") String tenantId,
            @RequestHeader("X-Department-ID") String departmentId,
            @RequestParam("file") MultipartFile file,
            jakarta.servlet.http.HttpServletRequest request) {

        UUID.fromString(departmentId); 
        UUID.fromString(tenantId);

        String userId = SecurityContextHolder.getContext().getAuthentication().getName();

        String xff = request.getHeader("X-Forwarded-For");
        String clientIp = (xff != null) ? xff.split(",")[0] : request.getRemoteAddr();

        if (file.isEmpty()) {
            return CompletableFuture.completedFuture(
                ResponseEntity.badRequest().body(Map.of("error", "El archivo está vacío"))
            );
        }

        String contentType = file.getContentType();
        if (contentType == null || !isValidContentType(contentType)) {
            return CompletableFuture.completedFuture(
                ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Formato no permitido. Solo PDF, JPG, PNG y CER."))
            );
        }

        log.info("Carga Segura iniciada por: {} | Organismo: {} | IP: {}", userId, tenantId, clientIp);

        return documentService.processUpload(file, tenantId, departmentId, userId, clientIp)
                .<ResponseEntity<?>>thenApply(result -> ResponseEntity.status(HttpStatus.CREATED).body(result))
                .exceptionally(e -> {
                    Throwable cause = (e.getCause() != null) ? e.getCause() : e;
                    log.error("Fallo en pipeline de seguridad/almacenamiento: {}", cause.getMessage());

                    if (cause instanceof SecurityException) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "Acceso denegado: Violación de aislamiento multitenant."));
                    }

                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(Map.of("error", "Error interno: " + cause.getMessage()));
                });
    }

    @GetMapping("/{file_hash}")
    public ResponseEntity<StreamingResponseBody> download(
            @PathVariable("tenant_id") String tenantId,
            @PathVariable("file_hash") String fileHash,
            @RequestHeader("X-Department-ID") String departmentId) {
    
        String cleanTenantId = (tenantId != null) ? tenantId.trim() : "";
        String cleanDeptId = (departmentId != null) ? departmentId.trim() : "";

        UUID.fromString(cleanTenantId);
        UUID.fromString(cleanDeptId);

        log.info("Descarga solicitada por mTLS - Hash: {}", fileHash);

        DocumentEntity doc = documentService.getMetadata(fileHash, cleanTenantId, cleanDeptId)
                .orElseThrow(() -> new NoSuchElementException("Documento no encontrado o sin privilegios de acceso."));

        StreamingResponseBody stream = documentService.downloadDocument(fileHash, cleanTenantId, cleanDeptId);

        String extension = getExtensionFromContentType(doc.getContentType());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileHash + extension + "\"")
                .contentType(MediaType.parseMediaType(doc.getContentType()))
                .body(stream);
    }

    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return "";
        return switch (contentType) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "application/x-x509-ca-cert", "application/pkix-cert" -> ".cer";
            default -> "";
        };
    }

    private boolean isValidContentType(String contentType) {
        return contentType.equals("application/pdf")
                || contentType.equals("image/jpeg")
                || contentType.equals("image/jpg")
                || contentType.equals("image/png")
                || contentType.equals("application/x-x509-ca-cert")
                || contentType.equals("application/pkix-cert");
    }
}