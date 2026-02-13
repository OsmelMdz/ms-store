package mx.gob.sda.ms_store.auth;

import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.util.Map;

@RestController
@RequestMapping("/v1/auth")
public class CertificateController {

    @Value("${server.ssl.key-store}")
    private Resource keyStoreResource;

    @Value("${server.ssl.key-store-password}")
    private String keyStorePassword;

    @Value("${server.ssl.key-alias}")
    private String keyAlias;

    private final TenantRegistrationService tenantService;

    public CertificateController(TenantRegistrationService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping("/public-key")
    public ResponseEntity<String> getPublicKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(keyStoreResource.getInputStream(), keyStorePassword.toCharArray());

        Certificate cert = keyStore.getCertificate(keyAlias);
        
        StringWriter sw = new StringWriter();
        try (JcaPEMWriter pw = new JcaPEMWriter(sw)) {
            pw.writeObject(cert);
        }
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "application/x-x509-ca-cert")
                .body(sw.toString());
    }

    @PostMapping("/tenant/register")
    public ResponseEntity<?> registerTenant(@RequestBody Map<String, String> requestBody) {
        try {
            String bootstrapToken = tenantService.registerNewTenant(requestBody);
            return ResponseEntity.ok(Map.of("bootstrap_token", bootstrapToken));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("error", e.getMessage()));
        }
    }
}