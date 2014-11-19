package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class DefaultResponseBodyWriter implements HttpResponseBodyWriter {

    private static final Logger log = LogManager.getLogger(DefaultResponseBodyWriter.class);

    private Map<MediaType, HttpResponseBodyWriter> writers = new HashMap<MediaType, HttpResponseBodyWriter>();

    private final HttpResponseBodyWriter defaultWriter;

    public DefaultResponseBodyWriter() {
        this(null);
    }
    
    public DefaultResponseBodyWriter(HttpResponseBodyWriter defaultWriter) {
        super();
        this.defaultWriter = defaultWriter;
    }

    @Override
    public void writeBody(HttpRequestMetaInfo requestMeta, HttpResponseMetaInfo responseMeta, Object entity, OutputStream os) throws IOException {

        if (entity instanceof byte[]) {
            IOUtils.write((byte[]) entity, os);
            return;
        }
        if (entity instanceof InputStream) {
            try (InputStream is = (InputStream) entity) {
                IOUtils.copy((InputStream) entity, os);
            }
            return;
        }
        if (entity.getClass().isPrimitive()) {
            os.write(entity.toString().getBytes(responseMeta.getEncoding()));
            return;
        }

        HttpResponseBodyWriter writer = writers.get(responseMeta.getMediaType());
        if (writer == null) {
            if (log.isDebugEnabled()) {
                log.debug("no writer for media type " + responseMeta.getMediaType());
            }
            writer = defaultWriter;
        }
        if (writer != null) {
            writer.writeBody(requestMeta,responseMeta, entity, os);
            return;
        }
        if (log.isDebugEnabled()) {
            log.debug("falling back to absolute default writer");
        }
        // oh dear, absolute fallback
        os.write(entity.toString().getBytes(responseMeta.getEncoding()));
        
        return;
    }

}