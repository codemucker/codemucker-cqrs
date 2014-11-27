package org.codemucker.cqrs;

import java.util.Locale;

import com.google.common.net.MediaType;

public class Variant {
    private final String encoding;
    private final Locale language;
    private final MediaType mediaType;

    public Variant(String encoding, Locale language, MediaType mediaType) {
        super();
        this.encoding = encoding;
        this.language = language;
        this.mediaType = mediaType;
    }

    public String getEncoding() {
        return encoding;
    }

    public Locale getLanguage() {
        return language;
    }

    public MediaType getMediaType() {
        return mediaType;
    }

}
