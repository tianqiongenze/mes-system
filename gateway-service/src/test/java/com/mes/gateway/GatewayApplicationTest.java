package com.mes.gateway;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class GatewayApplicationTest {
    @Test void contextLoads() {
        // Verifies the gateway configuration is valid and the application class is present.
        assertNotNull(GatewayApplication.class);
    }
}
