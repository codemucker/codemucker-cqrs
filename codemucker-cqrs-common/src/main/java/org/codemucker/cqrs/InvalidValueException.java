package org.codemucker.cqrs;

public class InvalidValueException extends MessageException {

    public InvalidValueException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidValueException(String message) {
        super(message);
    }

}
