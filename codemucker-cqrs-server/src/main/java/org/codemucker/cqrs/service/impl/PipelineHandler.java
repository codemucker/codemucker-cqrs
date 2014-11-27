package org.codemucker.cqrs.service.impl;

import org.codemucker.cqrs.service.DefaultHttpRequestContext;
import org.codemucker.cqrs.service.HttpResponse;

public interface PipelineHandler {

    HttpResponse invoke(DefaultHttpRequestContext request);

}
