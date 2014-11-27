package org.codemucker.cqrs.service;

public interface IMessageHandlerProvider {

    public  IMessageHandler getHandlerFor(Class<?> messageType);
}
