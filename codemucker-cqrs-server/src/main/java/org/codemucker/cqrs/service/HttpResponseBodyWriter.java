package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.OutputStream;

import com.google.inject.ImplementedBy;

@ImplementedBy(DefaultResponseBodyWriter.class)
public interface HttpResponseBodyWriter {

    void writeBody(HttpRequestMetaInfo requestMeta, HttpResponseMetaInfo responseMeta, Object entity, OutputStream os) throws IOException;

}
