package mx.gob.sda.ms_store.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.security.PublicKey;
import java.util.Base64;

@Component
public class JwtUtils {

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Claims validateToken(String token, PublicKey publicKey) {
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractTenantId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                throw new IllegalArgumentException("Formato JWT inválido.");
            }
            
            byte[] decodedBytes = Base64.getUrlDecoder().decode(parts[1]);
            String payload = new String(decodedBytes);
            
            JsonNode node = objectMapper.readTree(payload);
            
            if (node.has("sub")) {
                return node.get("sub").asText();
            } else {
                throw new RuntimeException("El token no contiene 'sub'.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error decodificando payload: " + e.getMessage());
        }
    }
}