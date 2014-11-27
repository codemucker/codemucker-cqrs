package org.codemucker.cqrs.service.json;

import java.io.IOException;
import java.io.OutputStream;

import org.codemucker.cqrs.service.HttpRequestMetaInfo;
import org.codemucker.cqrs.service.HttpResponseBodyWriter;
import org.codemucker.cqrs.service.HttpResponseMetaInfo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;

public class JacksonResponseBodyWriter implements HttpResponseBodyWriter {

    private final ObjectMapper objectMapper;
    
    @Inject
    public JacksonResponseBodyWriter(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    @Override
    public void writeBody(HttpRequestMetaInfo requestMeta, HttpResponseMetaInfo responseMeta, Object entity, OutputStream os) throws IOException {
        objectMapper.writeValue(os, entity);
    }

}
