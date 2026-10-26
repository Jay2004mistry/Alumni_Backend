package com.alumni.management.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class HealthControllerTest {

    private final HealthController healthController = new HealthController();

    @Test
    @DisplayName("Health endpoint returns UP status and timestamp")
    void testHealthEndpointReturnsUp() {
        ResponseEntity<Map<String, Object>> response = healthController.checkHealth();

        assertNotNull(response);
        assertEquals(200, response.getStatusCode().value());
        assertNotNull(response.getBody());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("GLS-Connect Alumni Backend", response.getBody().get("service"));
        assertNotNull(response.getBody().get("timestamp"));
    }

    @Test
    @DisplayName("Readiness endpoint returns UP when database connection is valid")
    void testReadinessEndpointReturnsUpWhenDbValid() throws Exception {
        HealthController controller = new HealthController();
        javax.sql.DataSource mockDs = org.mockito.Mockito.mock(javax.sql.DataSource.class);
        java.sql.Connection mockConn = org.mockito.Mockito.mock(java.sql.Connection.class);
        org.mockito.Mockito.when(mockDs.getConnection()).thenReturn(mockConn);
        org.mockito.Mockito.when(mockConn.isValid(2)).thenReturn(true);

        org.springframework.test.util.ReflectionTestUtils.setField(controller, "dataSource", mockDs);

        ResponseEntity<Map<String, Object>> response = controller.checkReadiness();
        assertEquals(200, response.getStatusCode().value());
        assertEquals("UP", response.getBody().get("status"));
        assertEquals("UP", response.getBody().get("database"));
    }

    @Test
    @DisplayName("Readiness endpoint returns 503 DOWN when database is unreachable")
    void testReadinessEndpointReturnsDownWhenDbFails() throws Exception {
        HealthController controller = new HealthController();
        javax.sql.DataSource mockDs = org.mockito.Mockito.mock(javax.sql.DataSource.class);
        org.mockito.Mockito.when(mockDs.getConnection()).thenThrow(new java.sql.SQLException("Connection refused"));

        org.springframework.test.util.ReflectionTestUtils.setField(controller, "dataSource", mockDs);

        ResponseEntity<Map<String, Object>> response = controller.checkReadiness();
        assertEquals(503, response.getStatusCode().value());
        assertEquals("DOWN", response.getBody().get("status"));
        assertEquals("DOWN", response.getBody().get("database"));
    }
}
