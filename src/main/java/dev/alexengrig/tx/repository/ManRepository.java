package dev.alexengrig.tx.repository;

import dev.alexengrig.tx.entity.ManEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import javax.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface ManRepository extends JpaRepository<ManEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ManEntity> findForUpdateById(Long id);
}
