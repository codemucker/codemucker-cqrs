package org.codemucker.cqrs.client;

public class CqrsException extends Exception {

    private static final long serialVersionUID = 1L;

    public CqrsException(String message, Throwable cause) {
        super(message, cause);
    }

    public CqrsException(String message) {
        super(message);
    }

}
