package org.codemucker.cqrs.service;

import org.codemucker.cqrs.InvalidValueException;

interface ValueConverter {
    Object convert(Object fromVal, Class<?> toType) throws InvalidValueException;
}