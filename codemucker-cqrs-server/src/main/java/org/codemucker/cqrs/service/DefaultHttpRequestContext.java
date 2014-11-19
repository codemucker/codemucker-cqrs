package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;

public class DefaultHttpRequestContext implements HttpRequestContext {
        private final Map<String, Object> properties = new HashMap<String, Object>();
        private final HttpRequest request;
        private Method handlerMethod;
        private String encoding;
        private Locale language;
        private MediaType mediaType;
        
        private String httpMethod;
        private SecurityContext securityContext;
        private HttpResponse response;

        public DefaultHttpRequestContext(HttpRequest request) {
            super();
            this.request = request;
        }
        
        public void setHandlerMethod(Method handlerMethod) {
            this.handlerMethod = handlerMethod;
        }

        @Override
        public Method getHandlerMethod() {
            return handlerMethod;
        }

        @Override
        public boolean hasProperty(String name) {
            return properties.containsKey(name);
        }

        @Override
        public Object getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public Collection<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public void setProperty(String name, Object value) {
            properties.put(name, value);
        }

        @Override
        public void removeProperty(String name) {
            properties.remove(name);
        }

        @Override
        public HttpRequest getRequest() {
            return request;
        }

        @Override
        public String getHttpMethod() {
            return httpMethod != null ? httpMethod : request.getHttpMethod();
        }

        @Override
        public void setHttpMethod(String method) {
            this.httpMethod = method;
        }

        @Override
        public MultivaluedMap<String, String> getHeaders() {
            return request.getHeaders();
        }

        @Override
        public Locale getLanguage() {
            return language !=null?language:request.getLanguage();
        }

        @Override
        public Map<String, Cookie> getCookies() {
            return request.getCookies();
        }

        @Override
        public SecurityContext getSecurityContext() {
            return this.securityContext;
        }

        @Override
        public void setSecurityContext(SecurityContext context) {
            this.securityContext = context;
        }

        @Override
        public void abortWith(HttpResponse response) {
            this.response = response;
        }

        public HttpResponse getResponse(){
            return response;
        }
        
        @Override
        public boolean isAborted() {
            return response != null;
        }

        @Override
        public String getPath() {
            return request.getPath();
        }

        @Override
        public List<String> getPathParts() {
            return request.getPathParts();
        }

        @Override
        public MultivaluedMap<String, String> getQueryParams() {
            return request.getQueryParams();
        }

        @Override
        public String getCharacterEncoding() {
            return encoding != null?encoding:request.getCharacterEncoding();
        }

        @Override
        public MediaType getMediaType() {
            return mediaType != null?mediaType:request.getMediaType();
        }

        @Override
        public List<MediaType> getAcceptableMediaTypes() {
            return request.getAcceptableMediaTypes();
        }

        @Override
        public List<Locale> getAcceptableLanguages() {
            return request.getAcceptableLanguages();
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return request.getInputStream();
        }

        @Override
        public boolean isSecure() {
            return request.isSecure();
        }

    }