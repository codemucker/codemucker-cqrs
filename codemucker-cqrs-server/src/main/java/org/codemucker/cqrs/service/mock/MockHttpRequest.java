package org.codemucker.cqrs.service.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;

import org.codemucker.cqrs.Variant;
import org.codemucker.cqrs.service.HttpRequest;
import org.codemucker.cqrs.service.HttpUtil;

import com.google.common.net.MediaType;

public class MockHttpRequest implements HttpRequest {

    private String httpMethod;
    private String path;
    private List<String> pathParts;
    
    private String characterEncoding;
    private MediaType mediaType;
    private Locale language;
    
    private List<MediaType> acceptMediaTypes = new ArrayList<>();
    private List<Locale> acceptLanguages = new ArrayList<>();
    
    private MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
    private MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    private Map<String, Cookie> cookies = new HashMap<>();
    
    private boolean secure;
    private InputStream inputStream;

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public List<String> getPathParts() {
        return pathParts;
    }

    @Override
    public MultivaluedMap<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public MultivaluedMap<String, String> getHeaders() {
        return headers;
    }

    @Override
    public Map<String, Cookie> getCookies() {
        return cookies;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return acceptMediaTypes;
    }

    @Override
    public Locale getLanguage() {
        return language;
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return acceptLanguages;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }
    


    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setPath(String path) {
        this.path = path;
        this.pathParts = HttpUtil.splitPath(path);
        
        if (path != null) {
            int queryIdx = path.indexOf('?');
            if (queryIdx != -1) {
                this.queryParams = HttpUtil.parseQueyString(path.substring(queryIdx + 1));
                return;
            }
        }
        
    }
    
    public void setVariant(Variant variant){
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

    public void setPathParts(List<String> pathParts) {
        this.pathParts = pathParts;
    }

    public void setMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public void setAcceptMediaTypes(List<MediaType> acceptMediaTypes) {
        this.acceptMediaTypes = acceptMediaTypes;
    }

    public void setLanguage(Locale language) {
        this.language = language;
    }

    public void setAcceptLanguages(List<Locale> acceptLanguages) {
        this.acceptLanguages = acceptLanguages;
    }

    public void setQueryParams(MultivaluedHashMap<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public void setHeaders(MultivaluedHashMap<String, String> headers) {
        this.headers = headers;
    }

    public void setCookies(Map<String, Cookie> cookies) {
        this.cookies = cookies;
    }

    public void setCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }
    
    public void mockSetBody(String body) {
//        try {
//            mockSetBody(body.getBytes("UTF-8"));
//        } catch (UnsupportedEncodingException e) {
//            throw new RuntimeException("couldn't convert body to UTF-8",e);
//        }
        mockSetBody(body.getBytes());
    }
    
    public void mockSetBody(byte[] bytes) {
        mockSetInputStream(new ByteArrayInputStream(bytes));
    }
    
    public void mockSetInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public void mockAddHeader(String name, String value) {
        if(headers == null){
            headers = new MultivaluedHashMap<>();
        }
        headers.add(name, value);
    }

    public void mockAddQueryParam(String name, String value) {
        if(queryParams == null){
            queryParams = new MultivaluedHashMap<>();
        }
        queryParams.add(name, value);
    }
    
    public void mockAddCookie(String name, String value) {
        if(cookies == null){
            cookies = new HashMap<>();
        }
        cookies.put(name, new Cookie(name, value));
    }
}
