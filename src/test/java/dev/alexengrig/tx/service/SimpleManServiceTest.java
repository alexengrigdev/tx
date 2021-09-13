package dev.alexengrig.tx.service;

import dev.alexengrig.tx.domain.Man;
import dev.alexengrig.tx.exception.NotFreeManException;
import dev.alexengrig.tx.helper.TestcontainersHelper;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
class SimpleManServiceTest {
    @Container
    static PostgreSQLContainer<?> postgres = TestcontainersHelper.createPostgreSQLContainer();

    @Autowired
    ManService service;

    @DynamicPropertySource
    static void postgresDatasourceSetup(DynamicPropertyRegistry registry) {
        TestcontainersHelper.setupDatasource(registry, postgres);
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
    void should_link_twoMen() throws NotFreeManException {
        Man juliet = service.create("Juliet");
        Man romeo = service.create("Romeo");
        service.link(juliet.getId(), romeo.getId());
        juliet = service.get(juliet.getId());
        assertEquals(romeo.getId(), juliet.getPartnerId(), "Juliet -> Romeo");
        romeo = service.get(romeo.getId());
        assertEquals(juliet.getId(), romeo.getPartnerId(), "Romeo -> Juliet");
    }

    @Test
    @SneakyThrows(InterruptedException.class)
    void should_link_loveTriangle() {
        Man cyclops = service.create("Cyclops");
        Man jeanGrey = service.create("Jean Grey");
        Man wolverine = service.create("Wolverine");
        AtomicInteger counter = new AtomicInteger();
        AtomicBoolean firstPairHasException = new AtomicBoolean();
        AtomicBoolean secondPairHasException = new AtomicBoolean();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try {
                counter.incrementAndGet();
                service.link(cyclops.getId(), jeanGrey.getId());
            } catch (NotFreeManException e) {
                firstPairHasException.set(true);
            }
        });
        executorService.submit(() -> {
            try {
                counter.incrementAndGet();
                service.link(jeanGrey.getId(), wolverine.getId());
            } catch (NotFreeManException e) {
                secondPairHasException.set(true);
            }
        });
        executorService.shutdown();
        if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            fail("Timeout expired");
        }
        assertEquals(2, counter.get(), "Number of method calls");
        Man updatedCyclops = service.get(cyclops.getId());
        Man updatedJeanGrey = service.get(jeanGrey.getId());
        Man updatedWolverine = service.get(wolverine.getId());
        if (firstPairHasException.get()) {
            assertEquals(updatedWolverine.getId(), updatedJeanGrey.getPartnerId(), "Jean Grey -> Wolverine");
            assertNull(updatedCyclops.getPartnerId(), "Cyclops");
        } else if (secondPairHasException.get()) {
            assertEquals(updatedJeanGrey.getId(), updatedCyclops.getPartnerId(), "Cyclops -> Jean Grey");
            assertNull(updatedWolverine.getPartnerId(), "Wolverine");
        } else {
            fail("Love triangle");
        }
    }
}