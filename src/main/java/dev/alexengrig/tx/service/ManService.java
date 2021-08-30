package dev.alexengrig.tx.service;

import dev.alexengrig.tx.domain.Man;

public interface ManService {
    Man create(String name);

    Man get(Long manId);

    void link(Long manId, Long anotherManId);
}
