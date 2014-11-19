package org.codemucker.cqrs.service;

import java.lang.reflect.Method;

public interface IMessageHandler{

    /**
     * Expose the method to be executed to allow for various request annotation processing
     */
    Method getMethod();
    
    /**
     * The message type this handler handles
     * 
     * @return
     */
    Class<?> getMessageType();
    
    Object invoke(HttpRequest request, Object requestBean);
}
