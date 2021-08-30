package dev.alexengrig.tx.service;

import dev.alexengrig.tx.domain.Man;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Testcontainers
class SimpleManServiceTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres")
            .withDatabaseName("txdb-it")
            .withUsername("txdb-it")
            .withPassword("txdb-it");

    @Autowired
    ManService service;

    @DynamicPropertySource
    static void postgresDatasourceSetup(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void should_setup() {
        assertTrue(postgres.isRunning(), "PostgreSQL isn't running");
        assertNotNull(service, "ManService");
    }

    @Test
    void should_create_manByName() {
        Man juliet = service.create("Juliet");
        assertNotNull(juliet.getId(), "Man's id");
        assertEquals("Juliet", juliet.getName(), "Man's name");
        assertNull(juliet.getPartnerId(), "Man's partner id");
    }

    @Test
    void should_get_manById() {
        Man juliet = service.create("Juliet");
        assertNotNull(juliet.getId(), "Man's id");
        juliet = service.get(juliet.getId());
        assertEquals("Juliet", juliet.getName(), "Man's name");
        assertNull(juliet.getPartnerId(), "Man's partner id");
    }

    @Test
    void should_link_twoMen() {
        Man juliet = service.create("Juliet");
        Man romeo = service.create("Romeo");
        service.link(juliet.getId(), romeo.getId());
        juliet = service.get(juliet.getId());
        assertEquals(romeo.getId(), juliet.getPartnerId(), "Juliet -> Romeo");
        romeo = service.get(romeo.getId());
        assertEquals(juliet.getId(), romeo.getPartnerId(), "Romeo -> Juliet");
    }
}