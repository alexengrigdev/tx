package dev.alexengrig.tx.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Man {
    private Long id;
    private String name;
    private Long partnerId;
}
