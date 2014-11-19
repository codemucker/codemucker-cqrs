package org.codemucker.cqrs.service;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.cqrs.InvalidValueException;
import org.codemucker.cqrs.MessageException;
import org.codemucker.cqrs.NotFoundException;

public class DefaultErrorResponseHandler  implements ErrorResponseHandler {

    private static final Logger log = LogManager.getLogger(DefaultErrorResponseHandler.class);
    
    @Override
    public HttpResponse toResponse(HttpRequest request,Throwable t) {
        if (!(t instanceof MessageException)) {
            t = new MessageException("Unknown error", t);
        }
        if (t instanceof InvalidValueException) {
            log(t);
            return DefaultHttpResponse.with().status(HttpStatus.BAD_REQUEST_400).entity(t).build();
        }
        if (t instanceof NotFoundException) {
            log(t);
            return DefaultHttpResponse.with().status(HttpStatus.NOT_FOUND_404).entity(t).build();
        }
        log.warn(t);
        return DefaultHttpResponse.with().status(HttpStatus.SERVER_ERROR_500).entity(t).build();
    }
    
    private void log(Throwable t){
        if (log.isDebugEnabled()) {
            log.debug(t);
        }
    }
}
