package mx.gob.sda.ms_store.document;

import jakarta.persistence.PostPersist;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DocumentAuditListener {

    @Autowired
    @Lazy
    private AuditLogRepository auditRepository;

    @PostPersist
    public void onPostPersist(DocumentEntity document) {
        try {
            AuditLogEntity logEntity = AuditLogEntity.builder()
                    .tableName("documents")
                    .operation("INSERT")
                    .rowId(document.getDocumentId().toString())
                    .clientIp(document.getClientIp())
                    .newData(String.format(
                        "{\"status\": \"%s\", \"file_hash\": \"%s\", \"tenant_id\": \"%s\"}", 
                        document.getStatus(), 
                        document.getFileHash(),
                        document.getTenantId()
                    ))
                    .build();
            
            auditRepository.save(logEntity);
        } catch (Exception e) {
            log.error("Fallo crítico al guardar auditoría: {}", e.getMessage());
        }
    }
}