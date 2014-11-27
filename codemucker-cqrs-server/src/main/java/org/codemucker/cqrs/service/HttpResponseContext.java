package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Variant;

public interface HttpResponseContext extends HttpResponse {

    void setStatus(HttpStatus status);

    void setStatus(int status);

    @Override
    OutputStream getOutputStream() throws IOException;

    @Override
    void flush() throws IOException;

    void abortWith(HttpResponse abortWith);

    void abortWith(Response jaxrsResponse);

    void setVariant(Variant variant);

    void setMediaType(MediaType type);

    void setCharacterEncoding(String encoding);

    void setLanguage(Locale locale);

    void setHeaders(MultivaluedMap<String, Object> headers);

    void setCookies(Map<String, NewCookie> cookies);

    boolean isCommitted();

}