package dev.alexengrig.tx.isolation;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
@SuppressWarnings({"SqlResolve", "SqlNoDataSourceInspection"})
public class JdbcPersonRepository implements PersonRepository {
    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Person> rowMapper;

    @Override
    public Person insert(int personId, String personName) {
        int rows = jdbcTemplate.update("""
                INSERT INTO person (id, name)
                VALUES (?, ?)
                """, personId, personName);
        assert rows == 1 : "Invalid result of insert: " + rows;
        return selectById(personId);
    }

    @Override
    public Person selectById(int personId) {
        return jdbcTemplate.queryForObject("""
                SELECT *
                FROM person
                WHERE id = ?
                """, rowMapper, personId);
    }

    @Override
    public List<Person> selectAll() {
        return jdbcTemplate.query("""
                SELECT *
                FROM person
                """, rowMapper);
    }

    @Override
    public boolean updateNameById(int personId, String newPersonName) {
        int rows = jdbcTemplate.update("""
                UPDATE person
                SET name = ?
                WHERE id = ?
                """, newPersonName, personId);
        return rows == 1;
    }

    @Override
    public List<Person> selectAllByNameStartsWith(String namePrefix) {
        return jdbcTemplate.query("""
                SELECT *
                FROM person
                WHERE name LIKE CONCAT(?, '%')
                """, rowMapper, namePrefix);
    }
}
