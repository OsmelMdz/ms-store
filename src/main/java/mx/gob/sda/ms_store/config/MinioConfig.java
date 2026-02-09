package mx.gob.sda.ms_store.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {
    
    @Value("${minio.endpoint}") 
    private String endpoint;
    
    @Value("${minio.access-key}") 
    private String accessKey;
    
    @Value("${minio.secret-key}") 
    private String secretKey;
    
    @Value("${minio.bucket}")
    private String bucket;
    
    @Value("${minio.part-size}")
    private long partSize;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
    
    @Bean
    public String minioBucket() {
        return bucket;
    }
    
    @Bean
    public long minioPartSize() {
        return partSize;
    }
}