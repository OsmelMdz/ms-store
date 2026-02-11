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
    @Column(name = "audit_id", updatable = false, nullable = false)
    private UUID auditId;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "operation", nullable = false)
    private String operation;

    @Column(name = "row_id")
    private String rowId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "old_data")
    private String oldData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_data")
    private String newData;

    @CreatedBy
    @Column(name = "changed_by_user")
    private String changedByUser;

    @JdbcTypeCode(SqlTypes.OTHER)
    @Column(name = "client_ip")
    @org.hibernate.annotations.ColumnTransformer(write = "?::inet")
    private String clientIp;

    @CreatedDate
    @Column(name = "fecha_hora", updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime fechaHora; 
}