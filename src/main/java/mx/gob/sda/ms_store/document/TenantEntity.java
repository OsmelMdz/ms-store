package mx.gob.sda.ms_store.document;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "catalogos_mirror")
@Data
public class TenantEntity {
    @Id
    @Column(name = "tenant_id")
    private UUID tenantId;
    private String name;
    private String status;
}