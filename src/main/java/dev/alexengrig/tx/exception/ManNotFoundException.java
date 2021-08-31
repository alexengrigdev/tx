package dev.alexengrig.tx.exception;

public class ManNotFoundException extends RuntimeException {
    public ManNotFoundException(Long manId) {
        super("No man by id: " + manId);
    }
}
