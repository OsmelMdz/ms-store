package mx.gob.sda.ms_store.document;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "documents", schema = "operational")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
@EntityListeners({AuditingEntityListener.class, DocumentAuditListener.class})
public class DocumentEntity {

    @Id
    @Column(name = "document_id", updatable = false, nullable = false)
    private UUID documentId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "file_hash", nullable = false, length = 64)
    private String fileHash;

    @Column(name = "file_size_bytes", nullable = false)
    private Long fileSizeBytes;

    @Column(name = "s3_object_key", nullable = false, length = 255)
    private String s3ObjectKey;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "content_type", length = 50)
    private String contentType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata")
    private String metadata; 

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "client_ip")
    @org.hibernate.annotations.ColumnTransformer(write = "?::inet")
    private String clientIp;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;
}