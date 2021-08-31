package dev.alexengrig.tx.service;

import dev.alexengrig.tx.domain.Man;
import dev.alexengrig.tx.entity.ManEntity;
import dev.alexengrig.tx.exception.ManNotFoundException;
import dev.alexengrig.tx.exception.NotFreeManException;
import dev.alexengrig.tx.repository.ManRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SimpleManService implements ManService {
    private final ManRepository repository;
    private final Converter<ManEntity, Man> converter;

    @Override
    public Man create(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        ManEntity entity = new ManEntity(name);
        ManEntity savedEntity = repository.save(entity);
        return converter.convert(savedEntity);
    }

    @Override
    public Man get(Long manId) {
        Objects.requireNonNull(manId, "Man id must not be null");
        ManEntity entity = repository.findById(manId).orElseThrow(() -> new ManNotFoundException(manId));
        return converter.convert(entity);
    }

    @Override
    @Transactional
    public void link(Long manId, Long anotherManId) throws NotFreeManException {
        Objects.requireNonNull(manId, "Man id must not be null");
        Objects.requireNonNull(manId, "Another man id must not be null");
        ManEntity man = getManForUpdateById(manId);
        ManEntity anotherMan = getManForUpdateById(anotherManId);
        requireBeFree(man);
        requireBeFree(anotherMan);
        man.setPartner(anotherMan);
        anotherMan.setPartner(man);
        repository.save(man);
        repository.save(anotherMan);
    }

    private ManEntity getManForUpdateById(Long manId) {
        return repository.findForUpdateById(manId).orElseThrow(() -> new ManNotFoundException(manId));
    }

    private void requireBeFree(ManEntity man) throws NotFreeManException {
        if (man.getPartner() != null) {
            throw new NotFreeManException(man.getId(), man.getPartner().getId());
        }
    }
}
