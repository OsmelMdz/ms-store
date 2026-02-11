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

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostPersist
    public void onPostPersist(DocumentEntity document) {
        try {
            String sql = "INSERT INTO operational.audit_logs (audit_id, table_name, operation, row_id, new_data, client_ip, changed_by_user, fecha_hora) " +
                         "VALUES (?, ?, ?, ?, ?::json, ?::inet, ?, CURRENT_TIMESTAMP)";
            
            jdbcTemplate.update(sql, 
                UUID.randomUUID(),
                "documents",
                "INSERT",
                document.getDocumentId().toString(),
                String.format("{\"status\": \"%s\", \"file_hash\": \"%s\", \"tenant_id\": \"%s\"}", 
                              document.getStatus(), document.getFileHash(), document.getTenantId()),
                document.getClientIp(),
                document.getCreatedBy()
            );
        } catch (Exception e) {
            log.error("Fallo en auditoría: {}", e.getMessage());
        }
    }
}