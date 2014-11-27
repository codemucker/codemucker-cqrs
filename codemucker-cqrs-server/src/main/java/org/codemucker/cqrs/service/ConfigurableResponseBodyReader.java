package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

public class ConfigurableResponseBodyReader implements HttpRequestBodyReader {

    private final Map<MediaType, HttpRequestBodyReader> readers;
    private final HttpRequestBodyReader defaultReader;

    public static Builder with() {
        return new Builder();
    }

    private ConfigurableResponseBodyReader(HttpRequestBodyReader defaultReader, Map<MediaType, HttpRequestBodyReader> readers) {
        this.defaultReader = defaultReader;
        this.readers = readers;
    }

    @Override
    public <T> T readBody(HttpRequestMetaInfo requestMeta, InputStream is, Class<T> entityType) throws IOException {
        HttpRequestBodyReader reader = getReader(requestMeta.getMediaType());
        if (reader != null) {
            return reader.readBody(requestMeta, is, entityType);
        }
        return null;
    }

    @Override
    public <T> T readBody(HttpRequestMetaInfo requestMeta, InputStream is, T entity) throws IOException {
        HttpRequestBodyReader reader = getReader(requestMeta.getMediaType());
        if (reader != null) {
            return reader.readBody(requestMeta, is, entity);
        }
        return entity;
    }

    private HttpRequestBodyReader getReader(MediaType mediaType) {
        HttpRequestBodyReader reader = readers.get(mediaType);
        if (reader == null) {
            reader = defaultReader;
        }
        return reader;
    }

    public static class Builder {
        private Map<MediaType, HttpRequestBodyReader> readers = new HashMap<>();
        private HttpRequestBodyReader defaultReader;

        public ConfigurableResponseBodyReader build() {
            return new ConfigurableResponseBodyReader(defaultReader, readers);
        }

        public Builder defaults() {
            return this;
        }

        public Builder defaultReader(HttpRequestBodyReader reader) {
            this.defaultReader = reader;
            return this;
        }

        public Builder reader(MediaType mediaType, HttpRequestBodyReader reader) {
            readers.put(mediaType, reader);
            return this;
        }
    }

}