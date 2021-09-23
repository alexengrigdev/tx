package dev.alexengrig.tx.isolation;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest
@Testcontainers
@SuppressWarnings("SqlResolve")
class ReadUncommittedTest {
    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql");

    @Autowired
    JdbcTemplate jdbcTemplate;
    @Autowired
    TransactionTemplate txTemplate;

    @DynamicPropertySource
    static void setMySQLDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getDatabaseName);
    }

    @Test
    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    void should_do_dirtyRead() {
        assertEquals(TransactionDefinition.ISOLATION_READ_UNCOMMITTED, txTemplate.getIsolationLevel(), "READ_UNCOMMITTED");

        int personId = 1;
        String personName = "Bill";
        Person person = createPerson(personId, personName);

        CountDownLatch onRollback = new CountDownLatch(1);
        CountDownLatch onCommit = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(() -> txTemplate.executeWithoutResult(status -> {
            System.out.println("1: Start in " + Thread.currentThread().getName());
            assertEquals(1, jdbcTemplate.update("""
                            UPDATE person
                            SET name = CONCAT('Dirty ', name)
                            WHERE id = ?
                            """, personId),
                    "Update of person");
            System.out.println("1: Updated");
            onRollback.countDown();
            System.out.println("1: Release 2");
            try {
                System.out.println("1: Wait 2");
                onCommit.await();
                System.out.println("1: Released");
            } catch (InterruptedException e) {
                fail(e);
            }
            status.setRollbackOnly();
            System.out.println("1: Rollback");
        }));
        Future<String> dirtyPersonNameFuture = executorService.submit(() -> txTemplate.execute(status -> {
            System.out.println("2: Start in " + Thread.currentThread().getName());
            try {
                System.out.println("2: Wait 1");
                onRollback.await();
                System.out.println("2: Released");
            } catch (InterruptedException e) {
                fail(e);
            }
            String name = jdbcTemplate.queryForObject("""
                    SELECT name
                    FROM person
                    WHERE id = ?
                    """, String.class, personId);
            System.out.println("2: Selected");
            System.out.println("2: Release 1");
            onCommit.countDown();
            assertNotNull(name, "Updated Person's name");
            return name;
        }));

        executorService.shutdown();
        if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            fail("Timeout expired");
        }

        String dirtyPersonName = dirtyPersonNameFuture.get();
        assertEquals("Dirty " + personName, dirtyPersonName, "Dirty Person's name");

        Person samePerson = findPersonById(personId);
        assertEquals(person.getName(), samePerson.getName(), "Person's name");
    }

    @Test
    void should_load() {
        assertTrue(mysql.isRunning(), "MySQL container must be running");
        assertNotNull(jdbcTemplate, "No JdbcTemplate");
    }

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.execute("""
                CREATE TABLE person (
                    id INT PRIMARY KEY,
                    name TEXT NOT NULL
                )
                """);
        txTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_UNCOMMITTED);
    }

    @AfterEach
    void afterEach() {
        jdbcTemplate.execute("DROP TABLE person");
    }

    @Test
    void should_create_person() {
        int personId = 1;
        String personName = "Tom";
        createPerson(personId, personName);
    }

    private Person createPerson(int personId, String personName) {
        assertEquals(1, jdbcTemplate.update("""
                        INSERT INTO person (id, name)
                        VALUES (?, ?)
                        """, personId, personName),
                "Insert of person");
        Person person = findPersonById(personId);
        assertNotNull(person, "Person");
        assertEquals(personId, person.getId(), "Person's id");
        assertEquals(personName, person.getName(), "Person's name");
        return person;
    }

    private Person findPersonById(int personId) {
        return jdbcTemplate.queryForObject("""
                SELECT *
                FROM person
                WHERE id = ?
                """, new PersonRowMapper(), personId);
    }
}
