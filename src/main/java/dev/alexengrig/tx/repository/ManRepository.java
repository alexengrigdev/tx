package dev.alexengrig.tx.repository;

import dev.alexengrig.tx.entity.ManEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ManRepository extends JpaRepository<ManEntity, Long> {
    Optional<ManEntity> findForUpdateById(Long id);
}
