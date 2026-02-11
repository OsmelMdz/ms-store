package mx.gob.sda.ms_store.document;

import jakarta.persistence.PostPersist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
public class DocumentAuditListener {

    @Autowired
    @Lazy
    private AuditLogRepository auditRepository;

    @PostPersist
    public void onPostPersist(DocumentEntity document) {
        AuditLogEntity log = AuditLogEntity.builder()
                .auditId(UUID.randomUUID())
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
        auditRepository.save(log);
    }
}