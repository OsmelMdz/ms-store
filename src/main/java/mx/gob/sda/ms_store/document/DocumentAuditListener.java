package mx.gob.sda.ms_store.document;

import jakarta.persistence.PostPersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Slf4j
@Component
public class DocumentAuditListener {
    private static AuditLogRepository auditRepository;

    @Autowired
    public void setAuditRepository(AuditLogRepository repository) {
        DocumentAuditListener.auditRepository = repository;
    }

    @PostPersist
    public void onPostPersist(DocumentEntity document) {
        try {
            AuditLogEntity logEntry = AuditLogEntity.builder()
                .tableName("documents")
                .operation("INSERT")
                .rowId(document.getDocumentId().toString())
                .newData(String.format(
                    "{\"status\": \"%s\", \"file_hash\": \"%s\", \"tenant_id\": \"%s\", \"content_type\": \"%s\"}",
                    document.getStatus(), 
                    document.getFileHash(), 
                    document.getTenantId(),
                    document.getContentType()
                ))
                .changedByUser(document.getCreatedBy())
                .clientIp(document.getClientIp())
                .build();

            auditRepository.save(logEntry);
            log.info("Auditoría registrada para el documento: {}", document.getDocumentId());
        } catch (Exception e) {
            log.error("Fallo crítico al persistir auditoría: {}", e.getMessage());
        }
    }
}