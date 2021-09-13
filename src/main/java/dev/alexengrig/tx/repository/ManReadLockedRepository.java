package dev.alexengrig.tx.repository;

import dev.alexengrig.tx.entity.ManEntity;
import org.springframework.stereotype.Repository;

@Repository
public interface ManReadLockedRepository extends ReadPessimisticLockedRepository<ManEntity, Long> {
}
