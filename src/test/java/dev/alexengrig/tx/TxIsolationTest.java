package dev.alexengrig.tx;

import dev.alexengrig.tx.domain.Man;
import dev.alexengrig.tx.helper.TestcontainersHelper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Testcontainers
class TxIsolationTest {
    @Container
    static PostgreSQLContainer<?> postgres = TestcontainersHelper.createPostgreSQLContainer();

    @Autowired
    JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void postgresDatasourceSetup(DynamicPropertyRegistry registry) {
        TestcontainersHelper.setupDatasource(registry, postgres);
    }

    @Test
    @SneakyThrows(InterruptedException.class)
    void dirtyRead() {
        Long manId = getNextManId();
        assertNotNull(manId, "Man id");
        String manName = "Bill";
        assertEquals(1, jdbcTemplate.update("INSERT INTO man (id, name) VALUES (?, ?)", manId, manName), "Insert man");
        Man man = getManById(manId);
        assertNotNull(man, "Man");
        assertEquals(manId, man.getId(), "Man id");
        assertEquals(manName, man.getName(), "Man name");
        assertNull(man.getPartnerId(), "Man partner id");

        CountDownLatch rollbackLatch = new CountDownLatch(1);
        CountDownLatch commitLatch = new CountDownLatch(1);

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(() -> {
            assertEquals(1, jdbcTemplate.update("UPDATE man SET name = 'Dirty ' || name WHERE id = ? ", manId));
            rollbackLatch.countDown();
            try {
                commitLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //TODO: rollback
        });
        executorService.execute(() -> {
            try {
                rollbackLatch.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            assertEquals(1, jdbcTemplate.update("UPDATE man SET name = 'The Best ' || name WHERE id = ? ", manId));
            commitLatch.countDown();
            //TODO: commit
        });
        executorService.shutdown();
        if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            fail("Timeout expired");
        }

        Man updatedMan = getManById(manId);
        assertEquals("The Best Dirty " + manName, updatedMan.getName(), "Man name");
    }

    private Long getNextManId() {
        return Objects.requireNonNull(jdbcTemplate.queryForObject("SELECT nextval('man_id_seq')", Long.class));
    }

    private Man getManById(Long manId) {
        return Objects.requireNonNull(jdbcTemplate.queryForObject("SELECT * FROM man WHERE id = ?", (rs, rowNum) -> {
            Long id = rs.getLong("id");
            if (rs.wasNull()) {
                id = null;
            }
            String name = rs.getString("name");
            Long partnerId = rs.getLong("partner_id");
            if (rs.wasNull()) {
                partnerId = null;
            }
            return new Man(id, name, partnerId);
        }, manId));
    }
}
