package org.codemucker.cqrs.service;

public interface HttpFilter {
    void onRequest(HttpRequestContext requestContext);
    void onResponse(HttpRequestContext requestContext,HttpResponse response);
    void onError(HttpRequestContext requestContext, Throwable t);
}
