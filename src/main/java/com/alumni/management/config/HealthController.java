package com.alumni.management.config;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private javax.sql.DataSource dataSource;

    @GetMapping
    public ResponseEntity<Map<String, Object>> checkHealth() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "GLS-Connect Alumni Backend",
            "timestamp", LocalDateTime.now().toString()
        ));
    }

    @GetMapping("/ready")
    public ResponseEntity<Map<String, Object>> checkReadiness() {
        boolean dbOk = false;
        if (dataSource != null) {
            try (java.sql.Connection conn = dataSource.getConnection()) {
                dbOk = conn.isValid(2);
            } catch (Exception ignored) {
                dbOk = false;
            }
        }
        if (dbOk) {
            return ResponseEntity.ok(Map.of(
                "status", "UP",
                "database", "UP",
                "service", "GLS-Connect Alumni Backend",
                "timestamp", LocalDateTime.now().toString()
            ));
        } else {
            return ResponseEntity.status(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE).body(Map.of(
                "status", "DOWN",
                "database", "DOWN",
                "service", "GLS-Connect Alumni Backend",
                "timestamp", LocalDateTime.now().toString()
            ));
        }
    }
}
