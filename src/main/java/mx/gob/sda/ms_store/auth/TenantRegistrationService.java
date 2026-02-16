package mx.gob.sda.ms_store.auth;

import lombok.RequiredArgsConstructor;
import mx.gob.sda.ms_store.document.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.vault.core.VaultTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
public class TenantRegistrationService {

    private final VaultTemplate vaultTemplate;
    private final TenantRepository tenantRepository;
    private final DepartmentRepository departmentRepository;
    private final JwtUtils jwtUtils;

    @Transactional
    public Map<String, Object> registerNewTenant(Map<String, Object> body) throws Exception {
        String tenantIdStr = (String) body.get("tenantId");
        String publicKeyPem = (String) body.get("publicKey");
        String signatureStr = (String) body.get("signature");
        String name = (String) body.getOrDefault("name", "ORGANISMO_NUEVO");
        Object deptoObj = body.get("departments");
        List<String> deptoNames = (deptoObj instanceof List) ? (List<String>) deptoObj : List.of("GENERAL");
        if (!jwtUtils.verifyExternalSignature(tenantIdStr, signatureStr, publicKeyPem)) {
            throw new SecurityException("Firma inválida: Intento de registro no autorizado.");
        }

        UUID tId = UUID.fromString(tenantIdStr);
        TenantEntity tenant = tenantRepository.findById(tId).orElseGet(() -> {
            TenantEntity nt = new TenantEntity();
            nt.setTenantId(tId);
            nt.setName(name);
            nt.setStatus("ACTIVE");
            return tenantRepository.save(nt);
        });
        List<Map<String, String>> createdDeptos = new ArrayList<>();
        for (String dName : deptoNames) {
            DepartmentEntity depto = new DepartmentEntity();
            depto.setDepartmentId(UUID.randomUUID());
            depto.setTenantId(tId);
            depto.setName(dName);
            depto.setActive(true);
            departmentRepository.save(depto);
            createdDeptos.add(Map.of("name", dName, "id", depto.getDepartmentId().toString()));
        }
        String bt = "BT-" + UUID.randomUUID();
        Map<String, Object> vaultPayload = Map.of(
            "publicKey", publicKeyPem,
            "bootstrapToken", bt,
            "rcsi", body.getOrDefault("rcsi", "OU-SDA")
        );
        vaultTemplate.write("secret/data/tenants/" + tenantIdStr, Map.of("data", vaultPayload));

        return Map.of(
            "tenantId", tenantIdStr,
            "bootstrap_token", bt,
            "departments", createdDeptos,
            "status", "SUCCESS"
        );
    }
}