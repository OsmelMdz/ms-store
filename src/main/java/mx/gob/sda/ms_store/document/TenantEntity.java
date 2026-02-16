package mx.gob.sda.ms_store.document;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table(name = "tenants", schema = "catalogos_mirror")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TenantEntity {
    @Id
    private UUID tenantId;
    private String name;
    private String status;
}