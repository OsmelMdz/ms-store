package mx.gob.sda.ms_store.auth;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import mx.gob.sda.ms_store.document.DocumentRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class TenantJwtFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final DocumentRepository documentRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("jakarta.servlet.request.X509Certificate");
        String authHeader = request.getHeader("Authorization");
        if (certs != null && certs.length > 0 && authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                PublicKey clientPublicKey = certs[0].getPublicKey(); 
                Claims claims = jwtUtils.validateToken(token, clientPublicKey);
                
                String tenantId = claims.getSubject();
                String actor = claims.get("actor", String.class);
                String deptId = request.getHeader("X-Department-ID");
                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        actor, null, Collections.emptyList());
                SecurityContextHolder.getContext().setAuthentication(auth);
                documentRepository.setSessionContext(tenantId, deptId != null ? deptId : "");

            } catch (Exception e) {
                SecurityContextHolder.clearContext();
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Firma de JWT no coincide con Certificado mTLS");
                return;
            }
        } else if (authHeader != null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Se requiere certificado mTLS para validar la sesion");
            return;
        }

        filterChain.doFilter(request, response);
    }
}