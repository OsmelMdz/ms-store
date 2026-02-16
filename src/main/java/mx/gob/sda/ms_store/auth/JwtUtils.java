package mx.gob.sda.ms_store.auth;

import io.jsonwebtoken.*;
import org.springframework.stereotype.Component;
import java.security.*;
import java.security.spec.*;
import java.util.*;

@Component
public class JwtUtils {

    public String createOfficialJwt(String tenantId, Map<String, Object> claims, String privKeyPem) throws Exception {
        PrivateKey priv = loadPrivateKey(privKeyPem);
        return Jwts.builder()
                .setSubject(tenantId)
                .addClaims(claims)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(priv, SignatureAlgorithm.RS256)
                .compact();
    }

    public boolean verifyExternalSignature(String data, String sig, String pubPem) throws Exception {
        Signature publicSignature = Signature.getInstance("SHA256withRSA");
        publicSignature.initVerify(loadPublicKey(pubPem));
        publicSignature.update(data.getBytes());
        return publicSignature.verify(Base64.getDecoder().decode(sig));
    }

    public Claims validateToken(String token, PublicKey pubKey) {
        return Jwts.parserBuilder().setSigningKey(pubKey).build().parseClaimsJws(token).getBody();
    }

    public PublicKey loadPublicKey(String pem) throws Exception {
        String clean = pem.replace("-----BEGIN PUBLIC KEY-----", "").replace("-----END PUBLIC KEY-----", "").replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(clean)));
    }

    private PrivateKey loadPrivateKey(String pem) throws Exception {
        String clean = pem.replace("-----BEGIN PRIVATE KEY-----", "").replace("-----END PRIVATE KEY-----", "").replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(clean)));
    }
}