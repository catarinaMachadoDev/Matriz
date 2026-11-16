package dev.matriz.shared;

import org.springframework.dao.DataAccessException;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ReadinessController {
    private final JdbcTemplate jdbc;

    ReadinessController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/ready")
    ResponseEntity<Void> readiness() {
        try {
            jdbc.queryForObject("select 1", Integer.class);
            return ResponseEntity.noContent().build();
        } catch (DataAccessException error) {
            return ResponseEntity.status(503).build();
        }
    }
}
