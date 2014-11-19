package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ServletHttpResponseAdapter implements HttpResponseContext {
    
    private static final Logger log = LogManager.getLogger(ServletHttpResponseAdapter.class);
    
    private final HttpServletResponse response;
    private int status;
    private boolean aborted = false;
    
    private MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
    private Map<String, NewCookie> cookies = new HashMap<>();

    private final ValueConverter valueConverter;
    
    public ServletHttpResponseAdapter(HttpServletResponse response,Variant defaultVariant, ValueConverter valueConverter) {
        this.response = response;
        this.valueConverter = valueConverter;
        setVariant(defaultVariant);
        setStatus(HttpStatus.OK_200);
    }

    @Override
    public void setStatus(HttpStatus status) {
        setStatus(status.getCode());
    }

    @Override
    public void setStatus(int status) {
        response.setStatus(status);
        this.status = status;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public void setVariant(Variant variant) {
        setLanguage(variant.getLanguage());
        setMediaType(variant.getMediaType());
        setCharacterEncoding(variant.getEncoding());
    }
    
    @Override
    public MediaType getMediaType() {
        return HttpUtil.toMediaType(response.getContentType(),null);
    }

    @Override
    public void setMediaType(MediaType val) {
        this.response.setContentType(val.toString());
    }

    @Override
    public String getEncoding() {
        return response.getCharacterEncoding();
    }

    @Override
    public void setCharacterEncoding(String val) {
        response.setCharacterEncoding(val);
    }

    @Override
    public void setLanguage(Locale locale) {
        response.setLocale(locale);
    }

    @Override
    public Locale getLanguage() {
        return response.getLocale();
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        return response.getOutputStream();
    }

    //@Override
    @Override
    public void abortWith(HttpResponse abortWith) {
        aborted = true;
        copyMeta(abortWith);
    }
    
    public void copyMeta(HttpResponseMetaInfo replaceWith) {
        response.reset();
        headers.clear();
        cookies.clear();
    
        cookies.putAll(replaceWith.getCookies());
        headers.putAll(replaceWith.getHeaders());
        if(replaceWith.getStatus() > 0){
            setStatus(replaceWith.getStatus());
        }
        if (replaceWith.getEncoding() != null) {
            setCharacterEncoding(replaceWith.getEncoding());
        }
        if (replaceWith.getMediaType() != null) {
            setMediaType(replaceWith.getMediaType());
        }
        if (replaceWith.getLanguage() != null) {
            setLanguage(replaceWith.getLanguage());
        }
    }

    //@Override
    @Override
    public void abortWith(Response abortWith) {
        aborted = true;
        response.reset();
        headers.clear();
        cookies.clear();
    
        cookies.putAll(abortWith.getCookies());
        headers.putAll(abortWith.getHeaders());
        if (abortWith.getStatus() > 0) {
            setStatus(abortWith.getStatus());
        }
        String encoding = abortWith.getHeaderString(HttpHeaderNames.CONTENT_ENCODING);
        if (encoding != null) {
            setCharacterEncoding(encoding);
        }
        if (abortWith.getMediaType() != null) {
            setMediaType(abortWith.getMediaType());
        }
        if (abortWith.getLanguage() != null) {
            setLanguage(abortWith.getLanguage());
        }
    }
    
    @Override
    public boolean isAborted() {
        return aborted || isCommitted();
    }

    @Override
    public void flush() throws IOException {
        flushMeta();
        this.response.flushBuffer();
    }

    public void flushMeta() {
        for(NewCookie cookie:cookies.values()){
            response.addCookie(HttpUtil.toServletCookie(cookie));
        }
        for(Entry<String, List<Object>> entry:headers.entrySet()){
            for(Object val:entry.getValue()){
                response.addHeader(entry.getKey(),toString(val));         
            }
        }
    }
    
    private String toString(Object val){
        if(valueConverter==null){
            return val==null?null:val.toString();
        }
        return (String)valueConverter.convert(val, String.class);
    }

    @Override
    public Object getEntity() {
        return null;
    }

    @Override
    public MultivaluedMap<String, Object> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, NewCookie> getCookies() {
        return cookies;
    }

    @Override
    public boolean isCommitted() {
        return response.isCommitted();
    }

    @Override
    public void setHeaders(MultivaluedMap<String, Object> headers) {
        this.headers = headers;
    }

    @Override
    public void setCookies(Map<String, NewCookie> cookies) {
        this.cookies = cookies;
    }

    
}