package org.codemucker.cqrs.service;

import com.google.inject.ImplementedBy;

@ImplementedBy(DefaultErrorResponseHandler.class)
public interface ErrorResponseHandler {

    public HttpResponse toResponse(HttpRequest request,Throwable t);
}
