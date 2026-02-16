package mx.gob.sda.ms_store.document;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param; 
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID; 
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {
    
    Optional<DocumentEntity> findByFileHash(String fileHash);

    @Query(value = "SELECT EXISTS(SELECT 1 FROM catalogos_mirror.departments WHERE department_id = :deptId AND tenant_id = :tenantId)", nativeQuery = true)
    boolean validateDepartmentInTenant(@Param("deptId") UUID deptId, @Param("tenantId") UUID tenantId);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query(value = "SELECT " +
            "set_config('app.current_tenant', NULLIF(:tenantId, ''), true), " +
            "set_config('app.current_department', NULLIF(:deptId, ''), true)", 
            nativeQuery = true)
    void setSessionContext(@Param("tenantId") String tenantId, @Param("deptId") String deptId);
}