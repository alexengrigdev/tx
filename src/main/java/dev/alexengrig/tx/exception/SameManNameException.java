package dev.alexengrig.tx.exception;

import java.text.MessageFormat;

public class SameManNameException extends RuntimeException {
    public SameManNameException(Long manId, String name) {
        super(MessageFormat.format("Man id={0} already has name \"{1}\"", manId, name));
    }
}
