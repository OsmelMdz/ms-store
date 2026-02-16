package mx.gob.sda.ms_store.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.bind.annotation.*;
import mx.gob.sda.ms_store.document.TenantRepository;
import mx.gob.sda.ms_store.document.TenantEntity;
import java.util.*;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final VaultTemplate vaultTemplate;
    private final JwtUtils jwtUtils;
    private final TenantRepository tenantRepository;

    public AuthController(VaultTemplate vaultTemplate, JwtUtils jwtUtils, TenantRepository tenantRepository) {
        this.vaultTemplate = vaultTemplate;
        this.jwtUtils = jwtUtils;
        this.tenantRepository = tenantRepository;
    }

    @PostMapping("/token")
    public ResponseEntity<?> exchange(@RequestBody Map<String, String> req) throws Exception {
        String tIdStr = req.get("tenantId");
        String bt = req.get("bootstrapToken");

        var vaultRes = vaultTemplate.read("secret/data/tenants/" + tIdStr);
        if (vaultRes == null) return ResponseEntity.status(401).body("Tenant no registrado");
        
        Map<String, Object> vData = (Map<String, Object>) vaultRes.getData().get("data");
        if (!bt.equals(vData.get("bootstrapToken"))) return ResponseEntity.status(401).body("BT Inválido");

        TenantEntity tenant = tenantRepository.findById(UUID.fromString(tIdStr))
                .orElseThrow(() -> new RuntimeException("Tenant no sincronizado en DB"));

        var sdaKeys = vaultTemplate.read("secret/data/ms-store/keys");
        String privKey = (String) ((Map)sdaKeys.getData().get("data")).get("privateKey");

        Map<String, Object> claims = new HashMap<>();
        claims.put("rcsi", vData.get("rcsi"));
        claims.put("sistema", tenant.getName());
        claims.put("actor", req.getOrDefault("actor", "SISTEMA_EXTERNO"));

        String token = jwtUtils.createOfficialJwt(tIdStr, claims, privKey);
        return ResponseEntity.ok(Map.of("access_token", token));
    }
}