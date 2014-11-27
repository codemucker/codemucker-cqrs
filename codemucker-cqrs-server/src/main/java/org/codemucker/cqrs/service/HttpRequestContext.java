package org.codemucker.cqrs.service;

import java.lang.reflect.Method;
import java.util.Collection;

import javax.ws.rs.core.SecurityContext;

public interface HttpRequestContext extends HttpRequest {

    void setHttpMethod(String method);

    HttpRequest getRequest();

    boolean hasProperty(String name);

    Object getProperty(String name);

    Collection<String> getPropertyNames();

    void setProperty(String name, Object value);

    void removeProperty(String name);

    Method getHandlerMethod();

    void setSecurityContext(SecurityContext context);

    SecurityContext getSecurityContext();

    void abortWith(HttpResponse response);
    
    boolean isAborted();
}