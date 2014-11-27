package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.OutputStream;

public interface HttpResponse extends HttpResponseMetaInfo {

    Object getEntity();

    OutputStream getOutputStream() throws IOException;

    boolean isAborted();

    void flush() throws IOException;

}