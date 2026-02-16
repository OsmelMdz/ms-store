package mx.gob.sda.ms_store.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/pki")
@RequiredArgsConstructor
public class PkiController {

    private final VaultTemplate vaultTemplate;

    @PostMapping("/sign")
    public ResponseEntity<String> signCsr(@RequestBody String csr) {
        Map<String, Object> request = Map.of(
            "csr", csr,
            "common_name", "Cliente-S3-Final",
            "ttl", "72h"
        );

        var response = vaultTemplate.write("pki/sign/microservicios-role", request);
        String certificate = (String) response.getData().get("certificate");
        
        return ResponseEntity.ok(certificate);
    }
}