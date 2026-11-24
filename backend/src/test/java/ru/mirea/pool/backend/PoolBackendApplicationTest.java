package ru.mirea.pool.backend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "POOL_TEST_DB_ENABLED", matches = "true")
class PoolBackendApplicationTest {

    @Test
    void loadsContextAndValidatesDatabaseSchema() {
    }
}
