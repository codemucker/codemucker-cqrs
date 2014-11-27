package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

public class DefaultHttpResponse implements HttpResponse {

    private final String characterEncoding;
    private final int status;
    private final Locale language;
    private final MediaType mediaType;
    private final Object entity;
    private MultivaluedMap<String, Object> headers;
    private Map<String, NewCookie> cookies;

    public DefaultHttpResponse(int status, MediaType mediaType, Locale language,String characterEncoding, Object entity, MultivaluedMap<String, Object> headers,
            Map<String, NewCookie> cookies) {
        super();
        this.status = status;
        this.mediaType = mediaType;
        this.characterEncoding = characterEncoding;
        this.language = language;
        this.entity = entity;
        this.headers = headers;
        this.cookies = cookies;
    }

    public static Builder with() {
        return new Builder();
    }
    
    public static Builder ok(){
        return with().status(HttpStatus.OK_200);
    }

    public static Builder status(HttpStatus status){
        return status(status.getCode());
    }
    
    public static Builder status(int status){
        return with().status(HttpStatus.OK_200);
    }

    
    @Override
    public boolean isAborted() {
        return entity != null && (entity instanceof HttpResponse || entity instanceof Response);
    }

//    @Override
//    public void abortWith(HttpResponse abortWithHttpResponse) {
//        this.abortWithHttpResponse = abortWithHttpResponse;
//    }
//
//    @Override
//    public void abortWith(Response abortWithResponse) {
//        this.abortWithResponse = abortWithResponse;
//    }

    @Override
    public String getEncoding() {
        return characterEncoding;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public Locale getLanguage() {
        return language;
    }
    
    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public Object getEntity() {
        return entity;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return cookies;
    }

//    @Override
//    public OutputStream getOutputStream() throws IOException {
//        return outputStream;
//    }

//    @Override
//    public void flush() throws IOException {
//        
//    }


    @Override
    public OutputStream getOutputStream() throws IOException {
        if (entity != null && entity instanceof OutputStream) {
            return (OutputStream) entity;
        }
        return null;
    }

    @Override
    public void flush() {
        //does nothing
    }

    public static class Builder {
        private String characterEncoding;
        private Locale language;
        private MediaType mediaType;
        
        private int status;
        
        private Object entity;

        private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
        private Map<String, NewCookie> cookies = new HashMap<>();

        public DefaultHttpResponse build() {
            return new DefaultHttpResponse(status, mediaType, language, characterEncoding, entity, headers, cookies);
        }

        public Builder variant(Variant variant) {
            if (variant.getMediaType() != null) {
                mediaType(variant.getMediaType());
            }
            if (variant.getLanguage() != null) {
                language(variant.getLanguage());
            }
            if (variant.getEncoding() != null) {
                characterEncoding(variant.getEncoding());
            }
            return this;
        }
        
        public Builder language(Locale language) {
            this.language = language;
            return this;
        }
        
        public Builder characterEncoding(String characterEncoding) {
            this.characterEncoding = characterEncoding;
            return this;
        }

        public Builder status(HttpStatus status) {
            status(status.getCode());
            return this;
        }
        
        public Builder status(int status) {
            this.status = status;
            return this;
        }

        public Builder mediaType(MediaType mediaType) {
            headerSingle(HttpHeaderNames.CONTENT_TYPE, mediaType.toString());
            return this;
        }

        public Builder outputStream(OutputStream outputStream) {
            entity(outputStream);
            return this;
        }
        
        public Builder abortWith(HttpResponse abortWithHttpResponse) {
            entity(abortWithHttpResponse);
            return this;
        }

        public Builder abortWith(Response abortWithResponse) {
            entity(abortWithResponse);
            return this;
        }
        
        public Builder entity(Object entity) {
            this.entity = entity;
            return this;
        }

        public Builder etag(String value) {
            headerSingle(HttpHeaderNames.ETAG, value);
            return this;
        }
        
        public Builder date(Date date) {
            headers.putSingle(HttpHeaderNames.DATE, date);
            return this;
        }
        
        public Builder expires(String value) {
            headers.putSingle(HttpHeaderNames.EXPIRES, value);
            return this;
        }
        
        public Builder header(String name, Object value) {
            headers.add(name, value);
            return this;
        }
        
        public Builder headerSingle(String name, Object value) {
            headers.putSingle(name,value);
            return this;
        }
        
        public Builder headers(MultivaluedMap<String, Object> headers) {
            this.headers = headers;
            return this;
        }

        public Builder cookie(String name, String value) {
            if( value==null){
                cookies.remove(name);
            } else {
                cookie(new NewCookie(name,value));
            }
            return this;
        }
        
        public Builder cookie(NewCookie  cookie) {
            this.cookies.put(cookie.getName(), cookie);
            return this;
        }
        
        public Builder cookies(Map<String, NewCookie> cookies) {
            this.cookies = cookies;
            return this;
        }
        
        public Builder copy(Response jaxResponse){
            headers.clear();
            headers.putAll(jaxResponse.getHeaders());
            cookies.clear();
            cookies.putAll(jaxResponse.getCookies());
            status(jaxResponse.getStatus());
            language(jaxResponse.getLanguage());
            mediaType(jaxResponse.getMediaType());
            return this;
        }
    }

}
