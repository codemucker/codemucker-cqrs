package org.codemucker.cqrs.service;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.ServletException;
import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codemucker.cqrs.CommandMessage;
import org.codemucker.cqrs.DELETE;
import org.codemucker.cqrs.GET;
import org.codemucker.cqrs.Ignore;
import org.codemucker.cqrs.InvalidValueException;
import org.codemucker.cqrs.MessageException;
import org.codemucker.cqrs.NotFoundException;
import org.codemucker.cqrs.OPTIONS;
import org.codemucker.cqrs.POST;
import org.codemucker.cqrs.PUT;
import org.codemucker.cqrs.Path;
import org.codemucker.cqrs.PathExpression;
import org.codemucker.cqrs.PathExpression.Var;
import org.codemucker.cqrs.QueryMessage;
import org.codemucker.lang.BeanNameUtil;
import org.codemucker.lang.annotation.ThreadSafe;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;

@ThreadSafe
public class DefaultRequestToMessageMapper implements IRequestToMessageMapper {

    private final ValueConverterFactory converterFactory;
    private final HttpRequestBodyReader bodyReader;

    private final Map<String, List<RequestBeanFactory>> requestBeanFactoriesByFirstPathPart = new HashMap<>();

    public static Builder with(){
        return new Builder();
    }
    
    private DefaultRequestToMessageMapper(ValueConverterFactory converterFactory, HttpRequestBodyReader bodyReader,Set<Class<?>> messageBeanTypes) {
        super();
        this.converterFactory = converterFactory;
        this.bodyReader = bodyReader;
        
        for(Class<?> messageType:messageBeanTypes){
            internalRegisterMessageBeanType(messageType);
        }
    }

    @Override
    public Object  mapToRequestBean(HttpRequest request) throws MessageException {
        Object bean;
        try {
            bean = createRequestBeanOrNull(request);
        } catch (Exception e) {
            throw new MessageException("error creating request bean for path " + request.getHttpMethod() + " " + request.getPath(), e);
        }
        if(bean == null){
            throw new NotFoundException("No request bean mapping for " + request.getHttpMethod() + " " + request.getPath() );
        }
        return bean;
    }

    private Object createRequestBeanOrNull(HttpRequest request) throws Exception {
        String key = newKey(request);
        if (key != null) {
            List<RequestBeanFactory> factories = requestBeanFactoriesByFirstPathPart.get(key);
            if (factories != null) {
                for (RequestBeanFactory factory : factories) {
                    if (factory.matches(request)) {
                        return factory.newPopulatedRequestBean(request);
                    }
                }
            }
        }
        return null;
    }

    private void internalRegisterMessageBeanType(Class<?> requestBeanClass) {
        boolean isCmd = isCmdType(requestBeanClass);
        String httpMethod = extractHttpMethod(requestBeanClass, isCmd);
        
        List<PathExpression> restPaths = extractRestPaths(requestBeanClass.getAnnotation(Path.class), requestBeanClass.getAnnotation(javax.ws.rs.Path.class));
        if (restPaths.size() == 0) {
            throw new IllegalArgumentException(String.format("expect atleast one rest path declared on %s. Use %s or %s", requestBeanClass.getName(),
                    Path.class.getName(), Path.class.getName()));
        }
        String contentType = MediaType.APPLICATION_JSON;

        for (PathExpression pathExpression : restPaths) {
            Map<String, FieldAdapter> fieldAdapters = extractFieldAdapters(requestBeanClass, isCmd, pathExpression);

            RequestBeanFactory beanFactory = new RequestBeanFactory(httpMethod, pathExpression, contentType, requestBeanClass, fieldAdapters, bodyReader);
            String firstPathPart = getFirstPathPart(pathExpression.expression);
            String key = newKey(httpMethod, firstPathPart);
            List<RequestBeanFactory> factories = requestBeanFactoriesByFirstPathPart.get(key);
            if (factories == null) {
                factories = new ArrayList<>();
                requestBeanFactoriesByFirstPathPart.put(key, factories);
            }
            factories.add(beanFactory);
        }
    }

    private static String getFirstPathPart(String path) {
        if(path.startsWith("/")){
            path = path.substring(1);
        }
        String firstPathPart = path.substring(0,path.indexOf('/'));
        if(firstPathPart.startsWith("/")){
            firstPathPart = firstPathPart.substring(1);
        }
        if(firstPathPart.endsWith("/")){
            firstPathPart = firstPathPart.substring(0,firstPathPart.length()-2);
        }
        return firstPathPart;
    }

    private boolean isCmdType(Class<?> requestBeanClass) {
        boolean isCmd = false;
        boolean isQuery = false;
        
        if (requestBeanClass.getSimpleName().contains("Cmd") || requestBeanClass.getSimpleName().contains("Command") || requestBeanClass.isAnnotationPresent(CommandMessage.class)) {
            isCmd = true;
        } 
        if (requestBeanClass.getSimpleName().contains("Query") || requestBeanClass.isAnnotationPresent(QueryMessage.class)) {
            isQuery = true;;
        } 
        if(!isCmd && !isQuery){
            throw new IllegalArgumentException("Couldn't figure out whether request class " + requestBeanClass
                    + " is a command or query. Name must contain either Cmd|Command|Query or be annotated with CommandMessage|QueryMessage");
        }
        if( isCmd && isQuery){
            throw new IllegalArgumentException("Couldn't figure out whether request class " + requestBeanClass
                    + " is a command or query as it seems to be both. Name must contain either _one_ of Cmd|Command|Query or subclass only _one_  CommandMessage|QueryMessage");
        }
        return isCmd;
    }

    private String extractHttpMethod(Class<?> requestBeanClass, boolean isCmd) {
        if (requestBeanClass.getAnnotation(GET.class) != null || requestBeanClass.getAnnotation(javax.ws.rs.GET.class) != null) {
            return "GET";
        }
        if (requestBeanClass.getAnnotation(POST.class) != null || requestBeanClass.getAnnotation(javax.ws.rs.POST.class) != null) {
            return "POST";
        }
        if (requestBeanClass.getAnnotation(OPTIONS.class) != null || requestBeanClass.getAnnotation(javax.ws.rs.OPTIONS.class) != null) {
            return "OPTIONS";
        }
        if (requestBeanClass.getAnnotation(DELETE.class) != null || requestBeanClass.getAnnotation(javax.ws.rs.DELETE.class) != null) {
            return "DELETE";
        }
        if (requestBeanClass.getAnnotation(PUT.class) != null || requestBeanClass.getAnnotation(javax.ws.rs.PUT.class) != null) {
            return "PUT";
        }
        if (!isCmd) {
            return "GET";
        }
        throw new IllegalArgumentException("No POST|PUT|DELETE|OPTIONS|GET annotation found on " + requestBeanClass.getName()
                + ". Either add one or make this bean a query type (defaults to GET)");
    }

    private String newKey(HttpRequest request) throws IOException, ServletException {
        Collection<String> parts = request.getPathParts();
        if (!parts.isEmpty()) {
            return newKey(request.getHttpMethod(), parts.iterator().next());
        }
        return null;
    }

    private String newKey(String httpMethod, String firstPathPart) {
        return httpMethod + ":" + firstPathPart;
    }

    private Map<String, FieldAdapter> extractFieldAdapters(Class<?> requestBeanClass, boolean isCmd,PathExpression expression) {
        Map<String, FieldAdapter> adapters = new HashMap<String, FieldAdapter>();
        extractFieldAdapters(requestBeanClass, adapters, isCmd, expression);
        return adapters;
    }

    private void extractFieldAdapters(Class<?> requestBeanClass, Map<String, FieldAdapter> fieldAdapters, boolean isCmd, PathExpression expression) {
        for (Field f : requestBeanClass.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers()) && !Modifier.isTransient(f.getModifiers()) && !Modifier.isNative(f.getModifiers()) && f.getAnnotation(Ignore.class) == null) {
                Method beanSetterMethod = null;
                String setterName = BeanNameUtil.toSetterName(f.getName());
                try {
                    beanSetterMethod = requestBeanClass.getMethod(setterName, new Class[] { f.getType() });
                } catch (NoSuchMethodException | SecurityException e) {
                    // no setter, use the field directly
                }

                if (beanSetterMethod == null && !Modifier.isPublic(f.getModifiers())) {
                    throw new IllegalArgumentException(String.format(
                            "field '%s' on %s is neither public nor has a public setter method of name %s. Either make public, create setter, or mark as ignore",
                            f.getName(), requestBeanClass.getName(), setterName));
                }
                String paramName = f.getName();
                FieldType type = FieldType.NONE;
                HeaderParam hp = f.getAnnotation(HeaderParam.class);
                if (hp != null) {
                    if (!hp.value().isEmpty()) {
                        paramName = hp.value();
                    }
                    type = FieldType.HEADER;
                }
                QueryParam qp = f.getAnnotation(QueryParam.class);
                if (qp != null) {
                    if (!qp.value().isEmpty()) {
                        paramName = qp.value();
                    }
                    type = FieldType.QUERY;
                }
                CookieParam cp = f.getAnnotation(CookieParam.class);
                if (cp != null) {
                    // TODO:extract path part idx
                    if (!cp.value().isEmpty()) {
                        paramName = cp.value();
                    }
                    type = FieldType.COOKIE;
                }
                PathParam pp = f.getAnnotation(PathParam.class);
                if (pp != null) {
                    // TODO:extract path part idx
                    if (!pp.value().isEmpty()) {
                        paramName = pp.value();
                    }
                    type = FieldType.PATH;
                }

                if (type == FieldType.NONE && !isCmd) {
                    type = FieldType.QUERY;
                }

                ValueConverter converter = getConverter(String.class, f.getType());
                FieldAdapter extractor = new FieldAdapter(type, paramName, converter,beanSetterMethod, f, expression);
                fieldAdapters.put(paramName, extractor);
            }
        }
        Class<?> superClass = requestBeanClass.getSuperclass();
        if (superClass != null & superClass != Object.class) {
            extractFieldAdapters(superClass, fieldAdapters, isCmd, expression);
        }
    }

    private ValueConverter getConverter(Class<?> from, Class<?> to) {
        return converterFactory.createFromTo(from, to);
    }

    private static List<PathExpression> extractRestPaths(Path restPath, javax.ws.rs.Path javaxPath) {
        List<PathExpression> expressions = new ArrayList<>();
        if (restPath != null) {
            for (String expression : restPath.value()) {
                expressions.add(PathExpression.parse(expression));
            }
        }
        if (javaxPath != null) {
            for (String expression : javaxPath.value().split("||")) {
                expressions.add(PathExpression.parse(expression));
            }
        }
        return expressions;
    }

    static enum FieldType {
        NONE, QUERY, HEADER, FORM_BODY, JSON_BODY, PATH, COOKIE;
    }

    static class RequestBeanFactory {
        private final PathExpression expression;
        private final Pattern pattern;
        private final String requestHttpMethod;
        private final Class<?> requestBeanType;
        //private final Map<String, FieldAdapter> fieldAdapters;
        private final FieldAdapter[] fieldAdaptersArray;
        
        private final HttpRequestBodyReader bodyReader;
        private final boolean readBody;
        
        public RequestBeanFactory(String requestHttpMethod, PathExpression expression, String requestContentType, Class<?> requestBeanType,
                Map<String, FieldAdapter> fieldAdapters, HttpRequestBodyReader bodyReader) {
            super();
            this.requestHttpMethod = requestHttpMethod;
            this.expression = expression;
            this.requestBeanType = requestBeanType;
            //this.fieldAdapters = fieldAdapters;
            this.fieldAdaptersArray = fieldAdapters.values().toArray(new FieldAdapter[] {});
            this.bodyReader = bodyReader;
            this.readBody = hasBody(requestHttpMethod) && bodyReader != null;
            String regexp = expressionToRegexp(expression, requestBeanType, fieldAdapters);
            this.pattern = Pattern.compile(regexp);
        }

        private String expressionToRegexp(PathExpression expression, Class<?> requestBeanType, Map<String, FieldAdapter> fieldAdapters) {
            // build up a regex like /foo/bar/[^/]+/
            StringBuilder sb = new StringBuilder();
            for (Object part : expression.getAllParts()) {
                if (part instanceof PathExpression.Var) {
                    String name = ((PathExpression.Var) part).name;
                    FieldAdapter adapter = fieldAdapters.get(name);
                    if (adapter == null) {
                        throw new IllegalArgumentException("Could not find field adapter with param name '" + name + "' for path " + expression.expression
                                + " for request bean type " + requestBeanType.getName() + ", have [" + Joiner.on(",").join(fieldAdapters.keySet()) + "]");
                    }
                    sb.append("[^/]+");
                } else {
                    sb.append(part.toString());
                }
            }
            String regex = "^" + sb.toString();
            if(!regex.endsWith("/")){
                regex+="/";
            }
            regex+= "?(\\?.*)?";
            return regex;
        }

        private static boolean hasBody(String httpMethod) {
            httpMethod = httpMethod.toUpperCase();
            return "POST".equals(httpMethod) || "PUT".equals(httpMethod) || "OPTION".equals(httpMethod);
        }
        
        public boolean matches(HttpRequest request) {
            return requestHttpMethod.equals(request.getHttpMethod()) && pattern.matcher(request.getPath()).matches();
        }

        public Object newPopulatedRequestBean(HttpRequest request) throws Exception {
            // TODO:how to deal with same properties in body and say query?
            // which one wins?
            // assume non body params win
            Object bean = newRequestBean();
            populateBeanFromBody(request,bean);
            setBeanProperties(request, bean);
            return bean;
        }

        private void populateBeanFromBody(HttpRequest request, Object bean) throws Exception {
            if (readBody && bodyReader != null) {
                try (InputStream is = request.getInputStream()) {
                    if (is != null) {
                        bodyReader.readBody(request, is, bean);
                    }
                }
            }
        }

        private void setBeanProperties(HttpRequest request, Object bean) throws Exception {
            for (int i = 0; i < fieldAdaptersArray.length; i++) {
                fieldAdaptersArray[i].extractAndSet(bean, request);
            }
        }

        private Object newRequestBean() throws MessageException {
            try {
                return requestBeanType.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                throw new MessageException("couldn't create new instance of " + requestBeanType.getName() + ", ensure it is public and has a no arg ctor", e);
            }
        }
    }

    private static class FieldAdapter {

        private final ValueConverter converter;
        private final String paramName;
        private final int pathPartIdx;
        private final FieldType valueType;
        private final Method beanMethodSetter;
        private final Field beanField;

        public FieldAdapter(FieldType valueType, String paramName, ValueConverter converter, Method beanMethodSetter, Field beanField, PathExpression expression) {
            super();
            this.valueType = valueType;
            
            this.paramName = paramName;
            this.converter = converter;
            this.beanMethodSetter = beanMethodSetter;
            this.beanField = beanField;
            
            Var var = expression.getVarNamed(paramName);
            if(var!=null){
                this.pathPartIdx = var.pathPartCount;
            } else {
                this.pathPartIdx = -1;
            }
        }

        public Class<?> getParamType() {
            return beanField.getType();
        }

        void extractAndSet(Object bean, HttpRequest req) throws MessageException, IOException, ServletException {
            // TODO:add form support
            
            switch (valueType) {
            case HEADER:
                extractAndSetHeaderParam(bean, req);
                break;
            case QUERY:
                extractAndSetQueryParam(bean, req);
                break;
            case COOKIE:
                extractAndSetCookieParam(bean, req);
                break;
            default:
                break;
            }
            extractAndSetPathParam(bean, req);
        }

        private void extractAndSetPathParam(Object bean, HttpRequest req) throws IOException, ServletException, MessageException, InvalidValueException {
            if(pathPartIdx ==-1){
                return;
            }
            List<String> parts = req.getPathParts();
            if (parts.size() > pathPartIdx) {
                String rawVal = parts.get(pathPartIdx);
                setBeanVal(bean, convertToBeanType(rawVal));
            }
        }

        private void extractAndSetHeaderParam(Object bean, HttpRequest req) throws MessageException, InvalidValueException {
            String rawVal = req.getHeaders().getFirst(paramName);
            if (rawVal != null) {
                setBeanVal(bean, convertToBeanType(rawVal));
            }
        }

        private void extractAndSetQueryParam(Object bean, HttpRequest req) throws MessageException, InvalidValueException {
            String rawVal = req.getQueryParams().getFirst(paramName);
            if (rawVal != null) {
                setBeanVal(bean, convertToBeanType(rawVal));
            }
        }

        private void extractAndSetCookieParam(Object bean, HttpRequest req) throws MessageException, InvalidValueException {
            javax.ws.rs.core.Cookie cookie = req.getCookies().get(paramName);    
            if (cookie != null) {
                String rawVal = cookie.getValue();
                setBeanVal(bean, convertToBeanType(rawVal));
            }
        
        }

        private Object convertToBeanType(String val) throws InvalidValueException {
            try {
                return converter.convert(val, getParamType());
            } catch (Exception e) {
                throw new InvalidValueException("Error converting value '" + val + "' to type " + getParamType() + " for field " + beanField.getName() + " on bean "
                        + beanField.getDeclaringClass().getSimpleName(), e);
            }
        }

        private void setBeanVal(Object bean, Object val) throws MessageException {
            if (beanMethodSetter != null) {
                try {
                    beanMethodSetter.invoke(bean, val);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new MessageException(String.format("Error invokign setter method named '%s' on request bean %s", beanMethodSetter.getName(), bean.getClass()
                            .getName()), e);
                }
            } else {
                try {
                    beanField.set(bean, val);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new MessageException(
                            String.format("Couldn't set value on field named '%s' on request bean %s", beanField.getName(), bean.getClass().getName()), e);
                }
            }
        }
    }
    
    public static class Builder {
        private ValueConverterFactory converterFactory;
        private HttpRequestBodyReader bodyReader;

        private Set<Class<?>> messageBeanTypes = new HashSet<>();

        
        public DefaultRequestToMessageMapper build(){
            Preconditions.checkNotNull(bodyReader, "expect bodyReader");
            Preconditions.checkNotNull(converterFactory, "expect converterFactory");
            
            return new DefaultRequestToMessageMapper(converterFactory, bodyReader, messageBeanTypes);
        }

        public Builder valueConverter(ValueConverterFactory converter){
            this.converterFactory = converter;
            return this;
        }
        
        public Builder bodyReader(HttpRequestBodyReader bodyReader){
            this.bodyReader = bodyReader;
            return this;
        }
        
        public Builder messageBean(Class<?> messageBeanType){
            this.messageBeanTypes.add(messageBeanType);
            return this;
        }
        
    }
}
