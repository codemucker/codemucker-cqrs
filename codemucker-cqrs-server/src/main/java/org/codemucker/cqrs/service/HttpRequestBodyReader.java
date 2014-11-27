package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.InputStream;

public interface HttpRequestBodyReader {
    <T> T readBody(HttpRequestMetaInfo requestMeta,InputStream is, Class<T> entityType) throws IOException;
    <T> T readBody(HttpRequestMetaInfo requestMeta,InputStream is, T entity) throws IOException;
    
}
