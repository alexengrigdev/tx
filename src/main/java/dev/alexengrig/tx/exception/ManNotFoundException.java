package dev.alexengrig.tx.exception;

import lombok.Getter;

@Getter
public class ManNotFoundException extends RuntimeException {
    private final Long manId;

    public ManNotFoundException(Long manId) {
        super("No man by id: " + manId);
        this.manId = manId;
    }
}
