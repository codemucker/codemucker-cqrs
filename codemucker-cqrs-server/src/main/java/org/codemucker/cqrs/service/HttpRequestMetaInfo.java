package org.codemucker.cqrs.service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MultivaluedMap;

import com.google.common.net.MediaType;

public interface HttpRequestMetaInfo {

    MultivaluedMap<String, String> getHeaders();

    Map<String, Cookie> getCookies();

    String getCharacterEncoding();
    
    MediaType getMediaType();

    List<MediaType> getAcceptableMediaTypes();

    Locale getLanguage();

    List<Locale> getAcceptableLanguages();
    
    String getHttpMethod();

    String getPath();

    List<String> getPathParts();

    MultivaluedMap<String, String> getQueryParams();

    boolean isSecure();


    
}
