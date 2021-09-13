package dev.alexengrig.tx.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@NoRepositoryBean
public interface WritePessimisticLockedRepository<T, ID> extends Repository<T, ID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<T> findById(ID id);

    <S extends T> S save(S entity);
}
