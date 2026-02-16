package mx.gob.sda.ms_store.config;

import mx.gob.sda.ms_store.auth.TenantJwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final TenantJwtFilter tenantFilter;

    public SecurityConfig(TenantJwtFilter tenantFilter) {
        this.tenantFilter = tenantFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .x509(x509 -> x509.subjectPrincipalRegex("CN=(.*?)(?:,|$)"))
            .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class)
            
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/v1/auth/tenant/register", "/v1/auth/public-key").permitAll() 
                .anyRequest().authenticated()
            );
            
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> new User(username, "", 
                AuthorityUtils.createAuthorityList("ROLE_USER"));
    }
}