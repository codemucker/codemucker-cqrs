package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.InputStream;

public interface HttpRequest extends HttpRequestMetaInfo {

    InputStream getInputStream() throws IOException;
    
    //Object getEntity();


}