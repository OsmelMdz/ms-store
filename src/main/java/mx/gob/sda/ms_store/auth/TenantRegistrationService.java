package mx.gob.sda.ms_store.auth;

import org.springframework.stereotype.Service;
import org.springframework.vault.core.VaultTemplate;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;

@Service
public class TenantRegistrationService {

    private final VaultTemplate vaultTemplate;

    public TenantRegistrationService(VaultTemplate vaultTemplate) {
        this.vaultTemplate = vaultTemplate;
    }

    public String registerNewTenant(Map<String, String> body) throws Exception {
        String tenantId = body.get("tenantId");
        String publicKeyPem = body.get("publicKey");
        String signatureStr = body.get("signature");

        if (tenantId == null || publicKeyPem == null || signatureStr == null) {
            throw new IllegalArgumentException("Faltan parámetros requeridos: tenantId, publicKey o signature");
        }

        boolean isValid = verifySignature(tenantId, signatureStr, publicKeyPem);
        
        if (!isValid) {
            throw new RuntimeException("Firma inválida. El registro ha sido rechazado por seguridad.");
        }

        String vaultPath = "secret/tenants/" + tenantId;
        Map<String, String> dataToSave = Map.of(
            "publicKey", publicKeyPem,
            "status", "ACTIVE"
        );
        
        vaultTemplate.write(vaultPath, dataToSave);

        return "BT-" + UUID.randomUUID().toString();
    }

    private boolean verifySignature(String data, String signatureStr, String publicKeyPem) throws Exception {
        String cleanKey = publicKeyPem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        PublicKey publicKey = kf.generatePublic(spec);

        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initVerify(publicKey);
        sig.update(data.getBytes());
        
        return sig.verify(Base64.getDecoder().decode(signatureStr));
    }
}