package org.codemucker.cqrs.service.json;

import java.io.IOException;
import java.io.InputStream;

import org.codemucker.cqrs.service.HttpRequestBodyReader;
import org.codemucker.cqrs.service.HttpRequestMetaInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

public class JacksonRequestBodyReader implements HttpRequestBodyReader{

    private final ObjectMapper objectMapper;
    
    @Inject
    public JacksonRequestBodyReader(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    @Override
    public <T> T readBody(HttpRequestMetaInfo requestMeta, InputStream is, Class<T> entityType) throws IOException {
        return objectMapper.readValue(is, entityType);
    }

    @Override
    public <T> T readBody(HttpRequestMetaInfo requestMeta, InputStream is, T entity) throws IOException {
        return objectMapper.readerForUpdating(entity).readValue(is);
    }


}
