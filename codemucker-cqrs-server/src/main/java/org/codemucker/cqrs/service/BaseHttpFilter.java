package org.codemucker.cqrs.service;

/**
 * Override just what you need
 */
public class BaseHttpFilter implements HttpFilter {

    @Override
    public void onRequest(HttpRequestContext requestContext) {
    }

    @Override
    public void onResponse(HttpRequestContext requestContext, HttpResponse response) {
    }

    @Override
    public void onError(HttpRequestContext requestContext, Throwable t) {
    }

}
