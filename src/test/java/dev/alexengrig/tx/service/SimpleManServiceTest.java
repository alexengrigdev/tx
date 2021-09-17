package dev.alexengrig.tx.service;

import dev.alexengrig.tx.domain.Man;
import dev.alexengrig.tx.exception.ManNotFoundException;
import dev.alexengrig.tx.exception.NotFreeManException;
import dev.alexengrig.tx.exception.SameManNameException;
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
        Man tom = service.create("Tom");
        assertNotNull(tom.getId(), "Man's id");
        tom = service.get(tom.getId());
        assertEquals("Tom", tom.getName(), "Man's name");
        assertNull(tom.getPartnerId(), "Man's partner id");
    }

    @Test
    void should_notFound_manById() {
        long manId = -1L;
        ManNotFoundException exception = assertThrows(ManNotFoundException.class, () -> service.get(manId));
        assertEquals(manId, exception.getManId(), "Man id");
    }

    @Test
    void should_update_man() {
        Man walterWhite = service.create("Walter White");
        assertEquals("Walter White", walterWhite.getName(), "Man name");
        Man heisenberg = service.update(walterWhite.getId(), "Heisenberg");
        assertEquals(walterWhite.getId(), heisenberg.getId(), "Man id");
        assertEquals("Heisenberg", heisenberg.getName(), "New man name");
    }

    @Test
    void should_update_manTwice() {
        String name = "Farrokh Bulsara";
        Man farrokhBulsara = service.create(name);
        assertEquals(name, farrokhBulsara.getName(), "Man name");
        String newName = "Freddie Mercury";
        Runnable updateTask = () -> {
            Man freddieMercury = service.update(farrokhBulsara.getId(), newName);
            assertEquals(farrokhBulsara.getId(), freddieMercury.getId(), "Man id");
            assertEquals(newName, freddieMercury.getName(), "New man name");
        };
        updateTask.run();
        SameManNameException exception = assertThrows(SameManNameException.class, updateTask::run);
        assertEquals(farrokhBulsara.getId(), exception.getManId());
        assertEquals(newName, exception.getManName());
    }

    @Test
    @SneakyThrows(InterruptedException.class)
    void should_update_manTwice_asynchronously() {
        String name = "Brian Hugh Warner";
        Man brianHughWarner = service.create(name);
        assertEquals(name, brianHughWarner.getName(), "Man name");
        String newName = "Marilyn Manson";
        AtomicReference<SameManNameException> firstException = new AtomicReference<>();
        AtomicReference<SameManNameException> secondException = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);
        Runnable updateTask = () -> {
            Man marilynManson = service.update(brianHughWarner.getId(), newName);
            assertEquals(brianHughWarner.getId(), marilynManson.getId(), "Man id");
            assertEquals(newName, marilynManson.getName(), "New man name");
        };
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.execute(() -> {
            try {
                latch.await();
                updateTask.run();
            } catch (SameManNameException e) {
                firstException.set(e);
            } catch (InterruptedException ignore1) {
                Thread.currentThread().interrupt();
            }
        });
        executorService.execute(() -> {
            try {
                latch.await();
                updateTask.run();
            } catch (SameManNameException e) {
                secondException.set(e);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        });
        latch.countDown();
        executorService.shutdown();
        if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            fail("Timeout expired");
        }
        SameManNameException exception;
        if (firstException.get() != null) {
            assertNull(secondException.get(), "Second exception");
            exception = firstException.get();
        } else if (secondException.get() != null) {
            assertNull(firstException.get(), "First exception");
            exception = secondException.get();
        } else {
            fail("No exception");
            return;
        }
        assertEquals(brianHughWarner.getId(), exception.getManId(), "Man id");
        assertEquals(newName, exception.getManName(), "Man name");
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
        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean firstPairHasException = new AtomicBoolean();
        AtomicBoolean secondPairHasException = new AtomicBoolean();
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        executorService.submit(() -> {
            try {
                latch.await();
                service.link(cyclops.getId(), jeanGrey.getId());
            } catch (NotFreeManException e) {
                firstPairHasException.set(true);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        });
        executorService.submit(() -> {
            try {
                latch.await();
                service.link(jeanGrey.getId(), wolverine.getId());
            } catch (NotFreeManException e) {
                secondPairHasException.set(true);
            } catch (InterruptedException ignore) {
                Thread.currentThread().interrupt();
            }
        });
        latch.countDown();
        executorService.shutdown();
        if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            fail("Timeout expired");
        }
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