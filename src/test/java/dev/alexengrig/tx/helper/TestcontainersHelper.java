package dev.alexengrig.tx.helper;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class TestcontainersHelper {
    public static final String DATABASE_NAME = "txdb";
    public static final String DATABASE_USERNAME = "txdb";
    public static final String DATABASE_PASSWORD = "txdb";

    public static PostgreSQLContainer<?> createPostgreSQLContainer() {
        return new PostgreSQLContainer<>("postgres")
                .withDatabaseName(DATABASE_NAME)
                .withUsername(DATABASE_USERNAME)
                .withPassword(DATABASE_PASSWORD);
    }

    public static void setupDatasource(DynamicPropertyRegistry registry, JdbcDatabaseContainer<?> database) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }
}
