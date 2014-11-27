package org.codemucker.cqrs.service;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;

public class HttpUtil {
    
    public static MultivaluedHashMap<String, String> parseQueyString(String q) {
        if(q==null){
            return new MultivaluedHashMap<>();
        }
        if(q.startsWith("?")){
            q = q.substring(1);
        }
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        try {
            String[] pairs = q.split("&");
            for (String pair : pairs) {
                int eq = pair.indexOf('=');
                if (eq == -1) {
                    String name = URLDecoder.decode(pair, ServletHttpRequestAdapter.ENCODING);
                    map.add(name, null);
                } else {
                    String name = URLDecoder.decode(pair.substring(0, eq), ServletHttpRequestAdapter.ENCODING);
                    String value = null;
                    if (eq < pair.length() - 1) {
                        value = URLDecoder.decode(pair.substring(eq + 1), ServletHttpRequestAdapter.ENCODING);
                    }
                    map.add(name, value);
                }
            }
        } catch (UnsupportedEncodingException e) {
            // never thrown
            throw new RuntimeException("error decoding query string", e);
        }
        return map;
    }
    
    public static List<String> splitPath(String path){
        List<String> paths = new ArrayList<>();
        if(path!=null){
            int queryIdx = path.indexOf('?');
            if(queryIdx !=-1){
                path = path.substring(0,queryIdx);
            }
            String[] parts = path.split("/");
            for(String part:parts){
                if(!part.isEmpty()){
                    paths.add(part);
                }
            }
        }
        return paths;
    }

    static MultivaluedMap<String, String> extractHeaders(HttpServletRequest req) {
        MultivaluedHashMap<String, String> map = new MultivaluedHashMap<>();
        for (Enumeration<String> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();) {
            String name = headerNames.nextElement();
            for (Enumeration<String> values = req.getHeaders(name); values.hasMoreElements();) {
                map.add(name, values.nextElement());
            }
        }
        return map;
    }
    
    static Map<String, Cookie> toCookies(javax.servlet.http.Cookie[] servletCookies) {
        Map<String, Cookie> cookies = new HashMap<>();
        if(servletCookies != null){
            for (javax.servlet.http.Cookie c : servletCookies) {
                Cookie c2 = new Cookie(c.getName(), c.getValue(), c.getPath(), c.getDomain(), c.getVersion());
                cookies.put(c2.getName(), c2);
            }
        }
        return cookies;
    }
    
    static javax.servlet.http.Cookie toServletCookie(NewCookie from) {
        javax.servlet.http.Cookie to = new javax.servlet.http.Cookie(from.getName(), from.getValue());
        to.setMaxAge(from.getMaxAge());
        to.setVersion(from.getVersion());
        
        if(from.getDomain() != null){
            to.setDomain(from.getDomain());
        }
        if(from.getPath() != null){
            to.setPath(from.getPath());
        }
        if(from.getComment() != null){
            to.setComment(from.getComment());
        }
        return to;
    }
    
    static List<Locale> toAcceptLanguages(Enumeration<Locale> locales,Locale defaultLocale) {
        List<Locale> list = new ArrayList<>(5);
        for (Enumeration<Locale> localesEnum = locales; locales.hasMoreElements();) {
            list.add(localesEnum.nextElement());
        }
        if(list.isEmpty() && defaultLocale!= null){
            list.add(defaultLocale);
        }
        return list;
    }
    
    static List<MediaType> toAcceptMediaTypes(Enumeration<String> contentTypes) {
        List<MediaType> list = new ArrayList<>(5);
        for (; contentTypes.hasMoreElements();) {
            MediaType type = toMediaType(contentTypes.nextElement(), null);
            if(type != null){
                list.add(type);
            }
        }
        return list;
    }
    
    static MediaType toMediaType(String contentType,MediaType defaultMediaType){
        if( contentType == null ){
            return defaultMediaType;
        }
        try {
            return MediaType.valueOf(contentType);
        } catch(IllegalArgumentException e){
//            if(log.isDebugEnabled()){
//                log.debug("unrecognized media type '" + contentType);
//            }
        }
        return defaultMediaType;
    }

}