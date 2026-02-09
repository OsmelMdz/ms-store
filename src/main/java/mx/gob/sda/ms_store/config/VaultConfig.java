package mx.gob.sda.ms_store.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.vault.authentication.TokenAuthentication;
import org.springframework.vault.client.VaultEndpoint;
import org.springframework.vault.core.VaultTemplate;

import java.net.URI;

@Configuration
public class VaultConfig {

    @Value("${spring.cloud.vault.uri}")
    private String vaultUri;

    @Value("${spring.cloud.vault.token}")
    private String vaultToken;

    @Bean
    public VaultTemplate vaultTemplate() {
        VaultEndpoint endpoint = VaultEndpoint.from(URI.create(vaultUri));
        return new VaultTemplate(endpoint, new TokenAuthentication(vaultToken));
    }

    @Bean
    public org.springframework.vault.core.VaultTransitOperations transitOperations(VaultTemplate vaultTemplate) {
        return vaultTemplate.opsForTransit();
    }
}