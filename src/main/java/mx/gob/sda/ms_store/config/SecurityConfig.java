package mx.gob.sda.ms_store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .x509(x509 -> x509
                .subjectPrincipalRegex("CN=(.*?)(?:,|$)") 
            )
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            );
            
        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            return new User(username, "", 
                AuthorityUtils.createAuthorityList("ROLE_USER"));
        };
    }
}