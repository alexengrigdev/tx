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

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
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
    @SneakyThrows(InterruptedException.class)
    void should_do_unrepeatableRead() {
        assertEquals(TransactionDefinition.ISOLATION_READ_UNCOMMITTED, txTemplate.getIsolationLevel(), "READ_UNCOMMITTED");

        Person jack = createPerson(1, "Jack");
        Person jacob = createPerson(2, "Jacob");
        Person john = createPerson(3, "John");
        String namePrefix = "Ja";
        assertTrue(jack.getName().startsWith(namePrefix), "Jack's name starts with 'Ja'");
        assertTrue(jacob.getName().startsWith(namePrefix), "Jacob's name starts with 'Ja'");
        assertFalse(john.getName().startsWith(namePrefix), "John's name doesn't start with 'Ja'");

        CountDownLatch onFirstFetch = new CountDownLatch(1);
        CountDownLatch onSecondFetch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(() -> txTemplate.executeWithoutResult(status -> {
            System.out.println("1: Start in " + Thread.currentThread().getName());
            Supplier<Set<String>> personNamesSupplier = () -> {
                List<Person> persons = jdbcTemplate.query("""
                        SELECT *
                        FROM person
                        WHERE name LIKE CONCAT(?, '%')
                        """, new PersonRowMapper(), namePrefix);
                assertEquals(2, persons.size(), "Number of persons");
                return persons.stream()
                        .map(Person::getName)
                        .collect(Collectors.toSet());
            };
            Set<String> names = personNamesSupplier.get();
            System.out.println("1: Selected");
            assertTrue(names.contains("Jack"), "List of name doesn't contain 'Jack'");
            assertTrue(names.contains("Jacob"), "List of name doesn't contain 'Jacob'");
            System.out.println("1: Release 2");
            onFirstFetch.countDown();
            try {
                System.out.println("1: Wait 2");
                onSecondFetch.await();
                System.out.println("1: Released");
            } catch (InterruptedException e) {
                fail(e);
            }
            names = personNamesSupplier.get();
            System.out.println("1: Selected again");
            assertFalse(names.contains("Jack"), "List of name contains 'Jack'");
            assertTrue(names.contains("Jackson"), "List of name doesn't contain 'Jackson'");
            assertTrue(names.contains("Jacob"), "List of name doesn't contain 'Jacob'");
        }));
        executorService.execute(() -> txTemplate.executeWithoutResult(status -> {
            System.out.println("2: Start in " + Thread.currentThread().getName());
            try {
                System.out.println("2: Wait 1");
                onFirstFetch.await();
                System.out.println("2: Released");
            } catch (InterruptedException e) {
                fail(e);
            }
            assertEquals(1, jdbcTemplate.update("""
                            UPDATE person
                            SET name = 'Jackson'
                            WHERE id = ?
                            """, jack.getId()),
                    "Update Jack's name");
            System.out.println("2: Updated");
            System.out.println("2: Release 1");
            onSecondFetch.countDown();
        }));

        executorService.shutdown();
        if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            fail("Timeout expired");
        }

        Person updatedJack = findPersonById(jack.getId());
        assertEquals("Jackson", updatedJack.getName(), "Updated Jack's name");
    }

    @Test
    @SneakyThrows({InterruptedException.class, ExecutionException.class})
    void should_do_phantomRead() {
        assertEquals(TransactionDefinition.ISOLATION_READ_UNCOMMITTED, txTemplate.getIsolationLevel(), "READ_UNCOMMITTED");

        Person tom = createPerson(1, "Tom");
        Person jerry = createPerson(2, "Jerry");

        Supplier<List<Person>> allPersonsSupplier = () -> jdbcTemplate.query("""
                SELECT *
                FROM person
                """, new PersonRowMapper());
        List<Person> allPersons = allPersonsSupplier.get();
        assertEquals(2, allPersons.size(), "Number of persons");

        CountDownLatch onFirstFetch = new CountDownLatch(1);
        CountDownLatch onSecondFetch = new CountDownLatch(1);
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        Future<List<Person>> personsFuture = executorService.submit(() -> txTemplate.execute(status -> {
            System.out.println("1: Start in " + Thread.currentThread().getName());
            List<Person> persons = allPersonsSupplier.get();
            System.out.println("1: Selected");
            assertEquals(2, persons.size(), "Number of persons");
            assertTrue(persons.contains(tom), "List of persons contains 'Tom'");
            assertTrue(persons.contains(jerry), "List of persons contains 'Jerry'");
            System.out.println("1: Release 2");
            onFirstFetch.countDown();
            try {
                System.out.println("1: Wait 2");
                onSecondFetch.await();
                System.out.println("1: Released");
            } catch (InterruptedException e) {
                fail(e);
            }
            persons = allPersonsSupplier.get();
            System.out.println("1: Selected again");
            assertEquals(3, persons.size(), "Number of persons");
            return persons;
        }));
        executorService.execute(() -> txTemplate.executeWithoutResult(status -> {
            System.out.println("2: Start in " + Thread.currentThread().getName());
            try {
                System.out.println("2: Wait 1");
                onFirstFetch.await();
                System.out.println("2: Released");
            } catch (InterruptedException e) {
                fail(e);
            }
            createPerson(3, "Spike");
            System.out.println("2: Inserted");
            System.out.println("2: Release 1");
            onSecondFetch.countDown();
        }));

        executorService.shutdown();
        if (!executorService.awaitTermination(3, TimeUnit.SECONDS)) {
            executorService.shutdownNow();
            fail("Timeout expired");
        }

        List<Person> persons = personsFuture.get();
        Set<String> names = persons.stream().map(Person::getName).collect(Collectors.toSet());
        assertTrue(names.contains("Tom"), "List of names doesn't contain 'Tom'");
        assertTrue(names.contains("Jerry"), "List of names doesn't contain 'Jerry'");
        assertTrue(names.contains("Spike"), "List of names doesn't contain 'Spike'");
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
