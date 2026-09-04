package de.minedesso.islewars.infrastructure.api;

public final class IsleWarsApiException extends RuntimeException {
    public IsleWarsApiException(String message) {
        super(message);
    }

    public IsleWarsApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
