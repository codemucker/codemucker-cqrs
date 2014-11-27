package org.codemucker.cqrs.service;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;

public class DefaultJacksonConfig {

    private final ObjectMapper objectMapper;

    public DefaultJacksonConfig(){
        objectMapper = new ObjectMapper().configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false).configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(JsonGenerator.Feature.QUOTE_FIELD_NAMES, false).configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true)
                .configure(JsonParser.Feature.ALLOW_COMMENTS, true).configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
                .configure(JsonParser.Feature.ALLOW_YAML_COMMENTS, true);

        objectMapper.setSerializationInclusion(Include.NON_EMPTY);
        // speedup json serialization using byte code manipulation
        // see https://github.com/FasterXML/jackson-module-afterburner
        objectMapper.registerModule(new AfterburnerModule());

        // SimpleModule testModule = new SimpleModule("MyModule",
        // Version.unknownVersion());//TODO:get group/artifactId
        // testModule.addSerializer(new SafeUriSerializer());
        // objectMapper.registerModule(testModule);
        //
    }

    public ObjectMapper getMapper() {
        return objectMapper;
    }

}
