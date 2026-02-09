package mx.gob.sda.ms_store.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing; 
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "mx.gob.sda.ms_store.document")
@EnableTransactionManagement
@EnableJpaAuditing 
public class JpaConfig {
}