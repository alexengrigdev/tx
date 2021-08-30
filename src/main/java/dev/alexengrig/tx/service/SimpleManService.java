package dev.alexengrig.tx.service;

import dev.alexengrig.tx.domain.Man;
import dev.alexengrig.tx.entity.ManEntity;
import dev.alexengrig.tx.repository.ManRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Service;

import java.text.MessageFormat;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class SimpleManService implements ManService {
    private final ManRepository repository;
    private final Converter<ManEntity, Man> converter;

    @Override
    public Man create(String name) {
        ManEntity man = new ManEntity(name);
        ManEntity savedMan = repository.save(man);
        return converter.convert(savedMan);
    }

    @Override
    public Man get(Long manId) {
        ManEntity entity = getManById(manId);
        return converter.convert(entity);
    }

    @Override
    public void link(Long manId, Long anotherManId) {
        Objects.requireNonNull(manId, "Man id must not be null");
        Objects.requireNonNull(manId, "Another man id must not be null");
        ManEntity man = getManById(manId);
        ManEntity anotherMan = getManById(anotherManId);
        requireBeFree(man);
        requireBeFree(anotherMan);
        man.setPartner(anotherMan);
        anotherMan.setPartner(man);
        repository.save(man);
        repository.save(anotherMan);
    }

    private ManEntity getManById(Long manId) {
        return repository.findById(manId).orElseThrow(() -> new IllegalArgumentException("No man by id: " + manId));
    }

    private void requireBeFree(ManEntity man) {
        if (man.getPartner() != null) {
            throw new IllegalStateException(MessageFormat.format("Man id={0} already has partner id={1}",
                    man.getId(), man.getPartner().getId()));
        }
    }
}
