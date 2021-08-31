package dev.alexengrig.tx.exception;

import java.text.MessageFormat;

public class NotFreeManException extends Exception {
    public NotFreeManException(Long manId, Long anotherManId) {
        super(MessageFormat.format("Man id={0} already has partner id={1}", manId, anotherManId));
    }
}
