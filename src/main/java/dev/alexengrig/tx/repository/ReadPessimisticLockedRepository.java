package dev.alexengrig.tx.repository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@NoRepositoryBean
public interface ReadPessimisticLockedRepository<T, ID> extends Repository<T, ID> {
    @Lock(LockModeType.PESSIMISTIC_READ)
    Optional<T> findById(ID id);
}
