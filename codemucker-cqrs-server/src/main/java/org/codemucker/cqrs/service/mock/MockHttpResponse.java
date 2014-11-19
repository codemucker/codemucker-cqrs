package org.codemucker.cqrs.service.mock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.codemucker.cqrs.service.HttpResponse;
import org.codemucker.cqrs.service.HttpResponseContext;
import org.codemucker.cqrs.service.HttpStatus;

public class MockHttpResponse implements HttpResponseContext {

    private int status;
    private boolean aborted;
    private String characterEncoding;
    private MediaType mediaType;
    private Locale language;
    private OutputStream outputStream = new ByteArrayOutputStream();
    private HttpResponse abortWithHttpResponse;
    private Response abortWithResponse;

    @Override
    public void setStatus(HttpStatus status) {
        setStatus(status.getCode());
    }

    @Override
    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public boolean isAborted() {
        return aborted;
    }

    @Override
    public void abortWith(HttpResponse abortWithHttpResponse) {
        this.abortWithHttpResponse = abortWithHttpResponse;
    }

    @Override
    public void abortWith(Response abortWithResponse) {
        this.abortWithResponse = abortWithResponse;
    }

    @Override
    public void setCharacterEncoding(String val) {
        this.characterEncoding = val;
    }

    @Override
    public String getEncoding() {
        return characterEncoding;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public void setMediaType(MediaType val) {
        this.mediaType = val;
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return outputStream;
    }

    @Override
    public void flush() throws IOException {
        if(outputStream!= null){
            outputStream.flush();
        }
    }

    public HttpResponse mockGetAbortWithHttpResponse() {
        return abortWithHttpResponse;
    }

    public Response mockGetAbortWithResponse() {
        return abortWithResponse;
    }

    public void mockSetAborted(boolean aborted) {
        this.aborted = aborted;
    }

    public void mockSetOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public Object getEntity() {
        return null;
    }

    @Override
    public Locale getLanguage() {
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return null;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return null;
    }

    @Override
    public void setVariant(Variant variant) {
        if (variant.getMediaType() != null) {
            setMediaType(variant.getMediaType());
        }
        if (variant.getLanguage() != null) {
            setLanguage(variant.getLanguage());
        }
        if (variant.getEncoding() != null) {
            setCharacterEncoding(variant.getEncoding());
        }
    }

    @Override
    public void setLanguage(Locale locale) {
        this.language = locale;
    }

    @Override
    public void setHeaders(MultivaluedMap<String, Object> headers) {
    }

    @Override
    public void setCookies(Map<String, NewCookie> cookies) {
    }

    @Override
    public boolean isCommitted() {
        return false;
    }
    
    

}
