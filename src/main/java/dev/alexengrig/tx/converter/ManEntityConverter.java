package dev.alexengrig.tx.converter;

import dev.alexengrig.tx.domain.Man;
import dev.alexengrig.tx.entity.ManEntity;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ManEntityConverter implements Converter<ManEntity, Man> {
    @Override
    public Man convert(ManEntity source) {
        Long id = source.getId();
        String name = source.getName();
        Long partnerId = Optional.ofNullable(source.getPartner()).map(ManEntity::getId).orElse(null);
        return new Man(id, name, partnerId);
    }
}
