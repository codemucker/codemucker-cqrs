package org.codemucker.cqrs;

import java.util.UUID;

public class MessageException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public final String errorId;
    
    public MessageException(String message, Throwable cause) {
        super(message, cause);
        this.errorId = UUID.randomUUID().toString();
    }

    public MessageException(String message) {
        super(message);
        this.errorId = UUID.randomUUID().toString();
    }
    
    public String getErrorId(){
        return errorId;
    }

}
