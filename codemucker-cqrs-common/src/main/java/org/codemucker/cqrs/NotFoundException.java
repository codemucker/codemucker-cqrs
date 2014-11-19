package org.codemucker.cqrs;

public class NotFoundException extends MessageException {

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotFoundException(String message) {
        super(message);
    }

}
