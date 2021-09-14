package dev.alexengrig.tx.service;

import dev.alexengrig.tx.domain.Man;
import dev.alexengrig.tx.entity.ManEntity;
import dev.alexengrig.tx.exception.ManNotFoundException;
import dev.alexengrig.tx.exception.NotFreeManException;
import dev.alexengrig.tx.exception.SameManNameException;
import dev.alexengrig.tx.repository.ManReadLockedRepository;
import dev.alexengrig.tx.repository.ManWriteLockedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SimpleManService implements ManService {
    private final ManWriteLockedRepository writeLockedRepository;
    private final ManReadLockedRepository readLockedRepository;
    private final Converter<ManEntity, Man> converter;

    @Override
    public Man create(String name) {
        Objects.requireNonNull(name, "Name must not be null");
        ManEntity entity = new ManEntity(name);
        ManEntity savedEntity = writeLockedRepository.save(entity);
        return converter.convert(savedEntity);
    }

    @Override
    @Transactional
    public Man get(Long manId) {
        Objects.requireNonNull(manId, "Man id must not be null");
        ManEntity entity = getMan(manId);
        return converter.convert(entity);
    }

    @Override
    @Transactional
    public Man update(Long manId, String name) {
        Objects.requireNonNull(manId, "Man id must not be null");
        Objects.requireNonNull(manId, "New man name must not be null");
        ManEntity entity = getManForUpdate(manId);
        requireNameNotEquals(manId, entity.getName(), name);
        entity.setName(name);
        ManEntity updatedEntity = writeLockedRepository.save(entity);
        return converter.convert(updatedEntity);
    }

    @Override
    @Transactional
    public void link(Long manId, Long anotherManId) throws NotFreeManException {
        Objects.requireNonNull(manId, "Man id must not be null");
        Objects.requireNonNull(manId, "Another man id must not be null");
        ManEntity man = getManForUpdate(manId);
        ManEntity anotherMan = getManForUpdate(anotherManId);
        requireBeFree(man);
        requireBeFree(anotherMan);
        man.setPartner(anotherMan);
        anotherMan.setPartner(man);
        writeLockedRepository.save(man);
        writeLockedRepository.save(anotherMan);
    }

    private ManEntity getMan(Long manId) {
        return readLockedRepository.findById(manId).orElseThrow(() -> new ManNotFoundException(manId));
    }

    private ManEntity getManForUpdate(Long manId) {
        return writeLockedRepository.findById(manId).orElseThrow(() -> new ManNotFoundException(manId));
    }

    private void requireNameNotEquals(Long manId, String oldName, String newName) {
        if (oldName.equals(newName)) {
            throw new SameManNameException(manId, newName);
        }
    }

    private void requireBeFree(ManEntity man) throws NotFreeManException {
        if (man.getPartner() != null) {
            throw new NotFreeManException(man.getId(), man.getPartner().getId());
        }
    }
}
