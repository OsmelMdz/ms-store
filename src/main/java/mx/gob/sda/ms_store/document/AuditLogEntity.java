package mx.gob.sda.ms_store.document;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "audit_logs", schema = "operational")
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "audit_id")
    private UUID auditId;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "operation")
    private String operation;

    @Column(name = "row_id")
    private String rowId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data")
    private String newData;

    @Column(name = "changed_by_user")
    private String changedByUser;

    @Column(name = "client_ip", columnDefinition = "inet")
    private String clientIp;

    @Column(name = "fecha_hora", updatable = false)
    private OffsetDateTime fechaHora;

    @PrePersist
    public void prePersist() {
        this.fechaHora = OffsetDateTime.now();
    }
}