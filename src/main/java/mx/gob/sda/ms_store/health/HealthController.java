package mx.gob.sda.ms_store.health;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
public class HealthController {

    @GetMapping
    public ResponseEntity<?> checkHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "message", "MS-STORE is running",
            "timestamp", System.currentTimeMillis()
        ));
    }
}