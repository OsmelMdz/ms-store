package mx.gob.sda.ms_store.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.vault.core.VaultTemplate;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

@Component
public class TenantJwtFilter extends OncePerRequestFilter {

    private final VaultTemplate vaultTemplate;
    private final JwtUtils jwtUtils;

    public TenantJwtFilter(VaultTemplate vaultTemplate, JwtUtils jwtUtils) {
        this.vaultTemplate = vaultTemplate;
        this.jwtUtils = jwtUtils;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String tenantId = jwtUtils.extractTenantId(token);
                
                var vaultResponse = vaultTemplate.read("secret/data/tenants/" + tenantId);
                
                if (vaultResponse != null && vaultResponse.getData() != null) {
                    Map<String, Object> dataWrapper = vaultResponse.getData();
                    Map<String, Object> actualSecrets = (Map<String, Object>) dataWrapper.get("data");
                    
                    if (actualSecrets != null && actualSecrets.containsKey("publicKey")) {
                        String pem = (String) actualSecrets.get("publicKey");
                        PublicKey pubKey = parsePublicKey(pem);
                        
                        jwtUtils.validateToken(token, pubKey);
                        
                        UsernamePasswordAuthenticationToken auth = 
                            new UsernamePasswordAuthenticationToken(tenantId, null, null);
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    } else {
                        System.err.println("ERROR: No se encontró 'publicKey' dentro del secreto en Vault");
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }
                } else {
                    System.err.println("ERROR: No existe el tenant '" + tenantId + "' en Vault");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    return;
                }
            } catch (Exception e) {
                System.err.println("ERROR DE VALIDACIÓN: " + e.getMessage());
                e.printStackTrace();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return;
            }
        }
        filterChain.doFilter(request, response);
    }

    private PublicKey parsePublicKey(String pem) throws Exception {
        String clean = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                          .replace("-----END PUBLIC KEY-----", "")
                          .replaceAll("\\s", "");
        return KeyFactory.getInstance("RSA")
                         .generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(clean)));
    }
}