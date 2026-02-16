package mx.gob.sda.ms_store.document;

import jakarta.persistence.*;
import lombok.Data;
import java.util.UUID;

@Entity
@Table(name = "departments", schema = "catalogos_mirror")
@Data
public class DepartmentEntity {
    @Id
    @Column(name = "department_id")
    private UUID departmentId;
    @Column(name = "tenant_id")
    private UUID tenantId;
    private String name;
    private String code;
    private boolean active;
}