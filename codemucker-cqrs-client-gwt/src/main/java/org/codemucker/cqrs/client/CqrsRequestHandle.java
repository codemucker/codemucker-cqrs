package org.codemucker.cqrs.client;

public interface CqrsRequestHandle {

    public void cancel();
    
    public boolean isPending();
}
