package org.codemucker.cqrs.service;

import javax.servlet.AsyncContext;

public class RequestContext {
    private final AsyncContext asyncContext;
    private final HttpRequest request;

    public RequestContext(AsyncContext asyncContext, HttpRequest request) {
        super();
        this.asyncContext = asyncContext;
        this.request = request;
    }

    public AsyncContext getAsyncContext() {
        return asyncContext;
    }

    public HttpRequest getRequest() {
        return request;
    }

}
