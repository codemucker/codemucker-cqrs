package org.codemucker.cqrs.service;

import java.util.Locale;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

public class VariantResolver {

    private final Variant defaultVariant;
    
    public VariantResolver(Variant defaultVariant){
        this.defaultVariant = defaultVariant;
    }
    
    public Variant getDefault(){
        return defaultVariant;
    }
    
    public Variant getResponseVariant(HttpRequest request, HttpResponse response){
        String charset = getResponseEncoding(request, response);
        MediaType mediaType = getResponseMediaType(request, response);
        Locale language = getResponseLanguage(request, response);
        
        return new Variant(mediaType,language,charset);
    }
    
    private String getResponseEncoding(HttpRequest request, HttpResponse response) {
        String charSet = null;
        if (response != null) {
            charSet = response.getEncoding();
        }
        if (charSet == null) {
            charSet = request.getCharacterEncoding();
        }
        if (charSet == null) {
            charSet = defaultVariant.getEncoding();
        }
        return charSet;
    }

    private MediaType getResponseMediaType(HttpRequest request, HttpResponse response) {
        MediaType mediaType = null;
        if(response != null){
            mediaType = response.getMediaType();
        }
        if (mediaType == null) {
            mediaType = request.getMediaType();
        }
        if (mediaType == null) {
            for (MediaType accept : request.getAcceptableMediaTypes()) {
                if (accept == MediaType.WILDCARD_TYPE) {
                    return accept;
                }
            }
            mediaType = defaultVariant.getMediaType();
        }
        return mediaType;
    }

    private Locale getResponseLanguage(HttpRequest request, HttpResponse response) {
        Locale language = null;
        if(response != null){
            language = response.getLanguage();
        }
        if (language == null) {
            language = request.getLanguage();
        }
        if (language == null) {
            for (Locale accept: request.getAcceptableLanguages()) {
                return accept;
            }
            language = defaultVariant.getLanguage();
        }
        return language;
    }

}