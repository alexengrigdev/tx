package dev.alexengrig.tx.exception;

import lombok.Getter;

import java.text.MessageFormat;

@Getter
public class SameManNameException extends RuntimeException {
    private final Long manId;
    private final String manName;

    public SameManNameException(Long manId, String name) {
        super(MessageFormat.format("Man id={0} already has name \"{1}\"", manId, name));
        this.manId = manId;
        this.manName = name;
    }
}
