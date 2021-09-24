package dev.alexengrig.tx.isolation;

import java.util.List;

public interface PersonRepository {
    Person insert(int personId, String personName);

    Person selectById(int personId);

    List<Person> selectAll();

    boolean updateNameById(int personId, String newPersonName);

    List<Person> selectAllByNameStartsWith(String namePrefix);

    boolean deleteById(int personId);
}
