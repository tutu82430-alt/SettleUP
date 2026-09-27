package io.settleup;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration smoke test: verifies the full Spring application context loads
 * cleanly against PostgreSQL and Redis service containers.
 */
@SpringBootTest
@ActiveProfiles("test")
class SettleUpApplicationTest {

    @Test
    void contextLoads() {
    }
}
