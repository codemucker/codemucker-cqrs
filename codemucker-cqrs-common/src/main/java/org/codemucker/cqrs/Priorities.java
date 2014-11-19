package org.codemucker.cqrs;

public final class Priorities {
    
    public static final int AUTHENTICATION = 1000;

    public static final int AUTHORIZATION = 2000;

    public static final int HEADERS = 3000;
    
    /**
     * After message entity has been deserialised from request
     */
    public static final int MESSAGE_DESERIALISED = 4000;
    
    /**
     * After handler has been matched for the given message
     */
    public static final int HANDLER_MATCHED = 4100;

    /**
     * After ACL has been applied
     */
    public static final int ACL = 4200;

    /**
     * After all other filters
     */
    public static final int USER = 5000;
}
