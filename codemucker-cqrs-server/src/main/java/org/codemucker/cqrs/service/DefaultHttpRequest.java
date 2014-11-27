package org.codemucker.cqrs.service;

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

import com.google.common.net.MediaType;

public class DefaultHttpRequest implements HttpRequest {

    private String httpMethod;
    private String path;
    private List<String> pathParts;
    
    private MediaType mediaType;
    private List<MediaType> acceptMediaTypes = new ArrayList<>();
    private Locale language;
    private List<Locale> acceptLanguages = new ArrayList<>();
    
    private MultivaluedHashMap<String, String> queryParams = new MultivaluedHashMap<>();
    private MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    private Map<String, Cookie> cookies = new HashMap<>();
    private String characterEncoding;
    private boolean secure;
    private ByteArrayInputStream inputStream;

    public DefaultHttpRequest(byte[] data){
        this(new ByteArrayInputStream(data));
    }
    
    public DefaultHttpRequest(ByteArrayInputStream inputStream){
        this.inputStream = inputStream;
    }
    
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
    


    public void mockSetHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void mockSetPath(String path) {
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

    public void mockSetPathParts(List<String> pathParts) {
        this.pathParts = pathParts;
    }

    public void mockSetMediaType(MediaType mediaType) {
        this.mediaType = mediaType;
    }

    public void mockSetAcceptMediaTypes(List<MediaType> acceptMediaTypes) {
        this.acceptMediaTypes = acceptMediaTypes;
    }

    public void mockSetLanguage(Locale language) {
        this.language = language;
    }

    public void mockSetAcceptLanguages(List<Locale> acceptLanguages) {
        this.acceptLanguages = acceptLanguages;
    }

    public void mockSetQueryParams(MultivaluedHashMap<String, String> queryParams) {
        this.queryParams = queryParams;
    }

    public void mockSetHeaders(MultivaluedHashMap<String, String> headers) {
        this.headers = headers;
    }

    public void mockSetCookies(Map<String, Cookie> cookies) {
        this.cookies = cookies;
    }

    public void mockSetCharacterEncoding(String characterEncoding) {
        this.characterEncoding = characterEncoding;
    }

    public void setSecure(boolean secure) {
        this.secure = secure;
    }

    public void addHeader(String name, String value) {
        if(headers == null){
            headers = new MultivaluedHashMap<>();
        }
        headers.add(name, value);
    }

    public void addQueryParam(String name, String value) {
        if(queryParams == null){
            queryParams = new MultivaluedHashMap<>();
        }
        queryParams.add(name, value);
    }
    
    public void addCookie(String name, String value) {
        if(cookies == null){
            cookies = new HashMap<>();
        }
        cookies.put(name, new Cookie(name, value));
    }
}
