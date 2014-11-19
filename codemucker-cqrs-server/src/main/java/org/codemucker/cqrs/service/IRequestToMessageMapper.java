package org.codemucker.cqrs.service;

import org.codemucker.cqrs.MessageException;
import org.codemucker.lang.annotation.ThreadSafe;

@ThreadSafe
public interface IRequestToMessageMapper {

    Object mapToRequestBean(HttpRequest request) throws MessageException;

}
