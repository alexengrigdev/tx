package dev.alexengrig.tx.isolation;

import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PersonRowMapper implements RowMapper<Person> {
    @Override
    public Person mapRow(ResultSet rs, int rowNum) throws SQLException {
        Integer id = rs.getInt("id");
        if (rs.wasNull()) {
            id = null;
        }
        String name = rs.getString("name");
        if (rs.wasNull()) {
            name = null;
        }
        return new Person(id, name);
    }
}
