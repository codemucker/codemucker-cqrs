package org.codemucker.cqrs;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class MediaType {
    
    
    public static final String APPLICATION_TYPE = "application";
    public static final String AUDIO_TYPE = "audio";
    public static final String HTML_TYPE = "html";
    public static final String IMAGE_TYPE = "image";
    public static final String JSON_TYPE = "json";
    public static final String PLAIN_TYPE = "plain";
    public static final String TEXT_TYPE = "text";
    public static final String VIDEO_TYPE = "video";
    
    public static final String WILDCARD = "*";
    
    public static final String CHARSET= "charset";
    public static final String UTF8= "utf8";
    
    public static final MediaType TEXT_PLAIN = new MediaType(TEXT_TYPE, PLAIN_TYPE, CHARSET, UTF8);
    public static final MediaType APPLICATION_JSON = new MediaType(APPLICATION_TYPE, JSON_TYPE, CHARSET, UTF8);

    private final String type;
    private final String subType;
    private final Map<String, String> parameters;
    private final String header;

    private MediaType(String type, String subType, String...parameters) {
        this(type,subType,toMap(parameters));
    }
    
    private MediaType(String type, String subType, Map<String, String> parameters) {
        super();
        this.type = type;
        this.subType = subType;
        this.parameters = parameters;
        this.header = toHeader();
    }

    private static Map<String,String> toMap(String[] keyValues){
        Map<String,String> map = new HashMap<>();
        for(int i =0; i < keyValues.length;i=i+2){
            map.put(keyValues[i], keyValues[i+1]);
        }
        return map;
    }
    
    private String toHeader(){
        StringBuilder sb = new StringBuilder();
        sb.append(type + "/" + subType);
        for (Entry<String, String> entry : parameters.entrySet()) {
            // TODO:do we escape?
            sb.append(";").append(entry.getKey()).append("=").append(entry.getValue());
        }
        return sb.toString();
    }
//
//    public static MediaType parse(String s){
//        
//    }
    
    @Override
    public String toString() {
        return header;
    }
}
