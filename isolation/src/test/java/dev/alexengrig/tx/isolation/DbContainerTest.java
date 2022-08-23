package dev.alexengrig.tx.isolation;

import org.junit.jupiter.api.Test;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class DbContainerTest {
    @SuppressWarnings("resource")
    @Container
    protected static final JdbcDatabaseContainer<?> DB_CONTAINER = new MySQLContainer<>("mysql")
            .withReuse(true);

    @DynamicPropertySource
    static void setDbContainerDataSource(DynamicPropertyRegistry registry) {
        System.out.println("Container id=" + DB_CONTAINER.getContainerId() +
                " and name=" + DB_CONTAINER.getContainerName());
        registry.add("spring.datasource.url", DB_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", DB_CONTAINER::getUsername);
        registry.add("spring.datasource.password", DB_CONTAINER::getDatabaseName);
    }

    @Test
    void should_run() {
        assertTrue(DB_CONTAINER.isRunning(), "Db container must be running");
    }
}
