package org.codemucker.cqrs.service;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.codemucker.cqrs.InvalidValueException;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class ValueConverterFactory implements ValueConverter{
    
    private Map<ValueConverterFactory.Key, ValueConverter> converters = new HashMap<>();
    //TODO:make configurable
    private final String DATE_FORMAT = "yyyyMMdd";
    private final String DATE_TIME_FORMAT = "yyyyMMddHHmmss";
    
    public ValueConverterFactory(){
        registerDefaultConverters();
    }
    
    private void registerDefaultConverters(){
        addConverter(String.class, String.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) {
                return fromVal;
            }
        });
        addConverter(String.class, int.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) {
                return fromVal == null?0:Integer.valueOf(fromVal.toString());
            }
        });
        addConverter(String.class, Integer.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) {
                return fromVal == null?null:Integer.valueOf(fromVal.toString());
            }
        });
        addConverter(String.class, boolean.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) throws InvalidValueException {
                if (fromVal != null) {
                    return toBoolean(fromVal.toString());
                }
                return false;
            }
        });
        addConverter(String.class, Boolean.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) throws InvalidValueException {
                if (fromVal != null) {
                    return toBoolean(fromVal.toString());
                }
                return null;
            }
        });
        addConverter(String.class, short.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) {
                return fromVal == null?0:Short.valueOf(fromVal.toString());
            }
        });
        addConverter(String.class, Short.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) {
                return fromVal == null?null:Short.valueOf(fromVal.toString());
            }
        });
        addConverter(String.class, float.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) {
                return fromVal == null?0:Float.valueOf(fromVal.toString());
            }
        });
        addConverter(String.class, Float.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) {
                return fromVal == null?null:Float.valueOf(fromVal.toString());
            }
        });
        ValueConverter defaultToStringConverter = new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) {
                return fromVal == null?null:fromVal.toString();
            }
        };
        for(Class<?> primType:new Class<?>[]{Object.class,String.class,Float.class,Integer.class,Boolean.class,Short.class,Character.class,Double.class}){
            addConverter(primType, String.class, defaultToStringConverter);
        }
        
        addConverter(String.class, Date.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) throws InvalidValueException {
                try {
                    return fromVal == null?null:newDateFormat().parse(fromVal.toString());
                } catch (ParseException e) {
                    throw new InvalidValueException("Could not parse date '" + fromVal + "'",e);
                }
            }
        });
        addConverter(Date.class, String.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) throws InvalidValueException {
                return fromVal == null?null:newDateFormat().format((Date)fromVal);
            }
        });
        addConverter(String.class, DateTime.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) throws InvalidValueException {
                return fromVal == null?null:DateTime.parse(fromVal.toString());
            }
        });
        
        addConverter(String.class, DateTime.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) throws InvalidValueException {
                return fromVal == null?null:newDateTimeFormatter().parseDateTime(fromVal.toString());
            }
        });
        addConverter(DateTime.class, String.class, new ValueConverter() {
            @Override
            public Object convert(Object fromVal,Class<?> toType) throws InvalidValueException {
                return fromVal == null?null:newDateTimeFormatter().print((DateTime)fromVal);
            }
        });
    }
    
    private DateFormat newDateFormat(){
        return new SimpleDateFormat(DATE_FORMAT);
    }
    
    private DateTimeFormatter newDateTimeFormatter(){
        return DateTimeFormat.forPattern(DATE_TIME_FORMAT);
    }
    
    
    private static boolean toBoolean(String fromVal) throws InvalidValueException {
        String v = fromVal.toLowerCase();
        if ("true".equals(v) || "t".equals(v) || "yes".equals(v) || "y".equals(v) || "1".equals(v) || "confirm".equals(v) || "enabled".equals(v)) {
            return true;
        }
        if ("false".equals(v) || "f".equals(v) || "no".equals(v) || "n".equals(v) || "0".equals(v) || "deny".equals(v) || "disabled".equals(v)) {
            return false;
        }
        throw new InvalidValueException("invalid boolean value '" + fromVal + "'");
    }
    
    void addConverter(Class<?> from, Class<?> to, ValueConverter converter){
        converters.put(new Key(from,to),converter);
    }

    @Override
    public Object convert(Object fromVal, Class<?> toType) throws InvalidValueException {
        Class<?> fromType = String.class;
        if(fromVal !=null){
            fromType = fromVal.getClass();
        }
        return createFromTo(fromType,toType);
    }
    
    public ValueConverter createFromTo(Class<?> fromType,Class<?> toType) throws NoSuchElementException {
        ValueConverterFactory.Key key = new Key(fromType,toType);
        ValueConverter converter = converters.get(key);
        if(converter !=null){
            return converter;
        }

        //TODO:look for static fromString(s), toString(x) on type
        throw new NoSuchElementException("Could not find converter to convert from type " + fromType + " to " + toType);
    }
    
    static class Key {
        final Class<?> from;
        final Class<?> to;
        public Key(Class<?> from, Class<?> to) {
            super();
            this.from = from;
            this.to = to;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((from == null) ? 0 : from.hashCode());
            result = prime * result + ((to == null) ? 0 : to.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ValueConverterFactory.Key other = (ValueConverterFactory.Key) obj;
            if (from == null) {
                if (other.from != null)
                    return false;
            } else if (!from.equals(other.from))
                return false;
            if (to == null) {
                if (other.to != null)
                    return false;
            } else if (!to.equals(other.to))
                return false;
            return true;
        }        
    }

}