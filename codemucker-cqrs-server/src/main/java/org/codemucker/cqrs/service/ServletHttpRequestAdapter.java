package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Variant;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class ServletHttpRequestAdapter implements HttpRequest {
    private static final Logger log = LogManager.getLogger(ServletHttpRequestAdapter.class);
    static final String ENCODING = "UTF-8";
    
    //TODO:make all readonly?
    private final HttpServletRequest request;
    private final String httpMethod;
    private final List<MediaType> acceptMediaTyes;
    private final MediaType mediaType;
    private final MultivaluedMap<String, String> queryParams;
    private final MultivaluedMap<String, String> headers;
    private final Map<String, Cookie> cookies;
    private final List<Locale> acceptLanguages;
    private final String characterEncoding;
    private final List<String> pathParts;
    private final InputStream inputStream;
    
    public ServletHttpRequestAdapter(HttpServletRequest request, Variant defaultVariant) throws IOException {
        this(request,defaultVariant,request.getInputStream());
    }
    public ServletHttpRequestAdapter(HttpServletRequest request, Variant defaultVariant, InputStream is) {
        super();
        this.request = request;
        this.inputStream = is;
        this.httpMethod = request.getMethod().toUpperCase();
        this.pathParts = HttpUtil.splitPath(request.getPathInfo());
        this.queryParams = HttpUtil.parseQueyString(request.getQueryString());
        this.headers = HttpUtil.extractHeaders(request);
        this.cookies = HttpUtil.toCookies(request.getCookies());
        this.mediaType = HttpUtil.toMediaType(request.getContentType(),defaultVariant.getMediaType());
        this.acceptMediaTyes = HttpUtil.toAcceptMediaTypes(request.getHeaders(HttpHeaderNames.ACCEPT));
        this.acceptLanguages = HttpUtil.toAcceptLanguages(request.getLocales(),defaultVariant.getLanguage());
        this.characterEncoding = request.getCharacterEncoding()==null?defaultVariant.getEncoding():request.getCharacterEncoding();
    }

    @Override
    public List<String> getPathParts() {
        return this.pathParts;
    }

    @Override
    public String getHttpMethod() {
        return httpMethod;
    }

    @Override
    public String getPath() {
        return request.getPathInfo();
    }

    @Override
    public MultivaluedMap<String, String> getQueryParams() {
        return queryParams;
    }

    @Override
    public String getCharacterEncoding() {
        return characterEncoding;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return inputStream;
    }

    @Override
    public boolean isSecure() {
        return request.isSecure();
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
    public MediaType getMediaType() {
        return mediaType;
    }

    @Override
    public List<MediaType> getAcceptableMediaTypes() {
        return acceptMediaTyes;
    }

    @Override
    public Locale getLanguage() {
        return request.getLocale();
    }

    @Override
    public List<Locale> getAcceptableLanguages() {
        return acceptLanguages;
    }
}