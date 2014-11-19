package org.codemucker.cqrs.service;

import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

public interface HttpResponseMetaInfo {

    int getStatus();

    Locale getLanguage();
    
    MediaType getMediaType();

    String getEncoding();

    MultivaluedMap<String, Object> getHeaders();
    
    Map<String, NewCookie> getCookies();
}
