package org.codemucker.cqrs.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codemucker.cqrs.MessageException;
import org.codemucker.cqrs.NotFoundException;
import org.codemucker.cqrs.Priority;
import org.codemucker.cqrs.service.impl.PipelineHandler;
import org.codemucker.cqrs.service.impl.RequestPipeline;
import org.codemucker.cqrs.service.impl.RequestQueue;
import org.codemucker.lang.annotation.Optional;
import org.codemucker.lang.annotation.Required;
import org.joda.time.Duration;

import com.google.common.base.Preconditions;

public class CqrsService implements Closeable {

    private static final Logger log = LogManager.getLogger(CqrsService.class);

    private static final String INVOKER_KEY = "org.codemucker.cqrs.invoker";
    private static final int MIN_BUF_SIZE = 1024;
    private static final int MAX_BUF_SIZE = MIN_BUF_SIZE * 10;
    private final Comparator<HttpFilter> FILTER_PRIORITY_COMPARATOR = new Comparator<HttpFilter>() {

        @Override
        public int compare(HttpFilter left, HttpFilter right) {
            return getPriority(left.getClass()) - getPriority(right.getClass());
        }
    
        private int getPriority(Class<?> type){
            Priority p =  type.getAnnotation(Priority.class);
            return p == null?0:p.value();
        }
    };
    

    private final VariantResolver variantResolver;
    private final long asyncTimeoutMs;

    private final IRequestToMessageMapper requestToMessageMapper;
    private final IMessageHandlerProvider messageHandlers;
    private final RequestPipeline requestPipeLine;
    private final HttpResponseBodyWriter responseBodyWriter;
    private final ErrorResponseHandler errorHandler;
    private final ValueConverterFactory converterFactory;

    public static Builder with() {
        return new Builder();
    }

    private final RequestQueue requestProcessingQueue = new RequestQueue() {
        @Override
        public void processRequest(RequestContext ctxt) {
            handleAsyncRequest(ctxt);
        }
    };

    private CqrsService(Variant defaultVariant, long asyncTimeoutMs, IMessageHandlerProvider messageHandlers, IRequestToMessageMapper requestToMessageMapper,
            HttpResponseBodyWriter responseWriter, ErrorResponseHandler errorHandler, ValueConverterFactory converterFactory,
            Collection<HttpFilter> filters) {
        super();
        this.asyncTimeoutMs = asyncTimeoutMs;
        this.variantResolver = new VariantResolver(defaultVariant);
        this.requestToMessageMapper = requestToMessageMapper;
        this.messageHandlers = messageHandlers;
        this.responseBodyWriter = responseWriter;
        this.errorHandler = errorHandler;
        this.converterFactory = converterFactory;

        List<HttpFilter> sortedFilters = new ArrayList<>(filters);
        sortedFilters.add(new SetHandlerFilter());
        Collections.sort(sortedFilters,FILTER_PRIORITY_COMPARATOR);
        
        this.requestPipeLine = RequestPipeline.with()
                .filters(sortedFilters)
                .handler(new PipelineHandler() {
                    @Override
                    public HttpResponse invoke(DefaultHttpRequestContext request) {
                        return invokeMessageHandlerAndWrapResponse(request);
                    }
                }).build();
    }
    
    @Priority(4099)
    private class SetHandlerFilter extends BaseHttpFilter{
        @Override
        public void onRequest(HttpRequestContext request) {
            setMessageHandler((DefaultHttpRequestContext) request);
        }
    }

    /**
     * The beginning of the request process. Start the whole thing off
     * 
     * @param request
     * @param response
     * @throws IOException
     */
    public void onRequest(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
        final AsyncContext async = request.startAsync();
        async.setTimeout(asyncTimeoutMs);
        async.addListener(new AsyncListener() {

            @Override
            public void onTimeout(AsyncEvent evt) throws IOException {
                log.debug("async timed out");
            }

            @Override
            public void onStartAsync(AsyncEvent evt) throws IOException {
                // push to processing queue
                log.debug("async started");
            }

            @Override
            public void onError(AsyncEvent evt) throws IOException {
                // push to error queue
                log.debug("async error", evt.getThrowable());
            }

            @Override
            public void onComplete(AsyncEvent evt) throws IOException {
                // write to output
                log.debug("async completed");
            }
        });
        
        final ServletInputStream input = request.getInputStream();
        boolean hasBody = hasBody(request.getMethod());
        
        final int contentLen = getBufferSize(request);
        if(hasBody && contentLen > 0){
            readEntityStreamAsync(request, async, input, contentLen);
        } else {
            HttpRequest wrappedRequest = new ServletHttpRequestAdapter(request, variantResolver.getDefault(), null);
            try {
                pushRequest(new RequestContext(async, wrappedRequest));
            } catch (InterruptedException e) {
                log.warn(e);
            }
        }
       
    }

    private void readEntityStreamAsync(final HttpServletRequest request, final AsyncContext async, final ServletInputStream input, final int contentLen) {
        input.setReadListener(new ReadListener() {
            final ByteArrayOutputStream buf = new ByteArrayOutputStream(contentLen);
   
            @Override
            public void onError(Throwable t) {
                // never mind, ignore
                log.debug("error reading client", t);
            }
   
            @Override
            public void onDataAvailable() throws IOException {
                int readLen;
                byte[] bytes = new byte[1024];
   
                while (input.isReady() && (readLen = input.read(bytes)) != -1) {
                    buf.write(bytes, 0, readLen);
                }
            }
   
            @Override
            public void onAllDataRead() throws IOException {
                // push request onto queue
                HttpRequest wrappedRequest = new ServletHttpRequestAdapter(request, variantResolver.getDefault(), contentLen==0?null:new ByteArrayInputStream(buf.toByteArray()));
                try {
                    pushRequest(new RequestContext(async, wrappedRequest));
                } catch (InterruptedException e) {
                    // TODO:too many requests!
                    log.warn(e);
                }
            }
        });
    }
    
    private boolean hasBody(String method){
        if("GET".equals(method) || "OPTIONS".equals(method)|| "HEAD".equals(method)){
            return false;
        }
        return true;
    }

    private int getBufferSize(HttpServletRequest request) {
        int len = request.getContentLength();
        if (len < MIN_BUF_SIZE) {
            len = MIN_BUF_SIZE;
        }
        if (len > MAX_BUF_SIZE) {
            len = MAX_BUF_SIZE;
        }
        return len;
    }

    private void pushRequest(RequestContext ctxt) throws InterruptedException {
        requestProcessingQueue.push(ctxt);
    }

    private void handleAsyncRequest(final RequestContext ctxt) {
        DefaultHttpRequestContext httpContext = new DefaultHttpRequestContext(ctxt.getRequest());
        HttpResponse response;
        Variant variant;
        Object entity;
        try {
            response = requestPipeLine.invoke(httpContext);
            variant = variantResolver.getResponseVariant(ctxt.getRequest(), response);
        } catch (Throwable t) {
            response = errorHandler.toResponse(httpContext, t);
            variant = variantResolver.getResponseVariant(ctxt.getRequest(), response);
        }
        entity = response.getEntity();

        HttpServletResponse servletResponse = (HttpServletResponse) ctxt.getAsyncContext().getResponse();
        final ServletHttpResponseAdapter responseAdapter = new ServletHttpResponseAdapter(servletResponse, variant, converterFactory);

        responseAdapter.copyMeta(response);
        // now write converted output

        try {
            // convert entity to stream
            final ByteArrayOutputStream bufferOut = new ByteArrayOutputStream();
            responseBodyWriter.writeBody(ctxt.getRequest(), response, entity, bufferOut);
            final ByteArrayInputStream bufferIn = new ByteArrayInputStream(bufferOut.toByteArray());
            // write response in background
            final ServletOutputStream servletOut = servletResponse.getOutputStream();

            servletOut.setWriteListener(new WriteListener() {
                @Override
                public void onWritePossible() throws IOException {
                    // write headers
                    try {
                        responseAdapter.flush();
                        byte[] bytes = new byte[1024];
                        int readBytes;
                        while ((readBytes = bufferIn.read(bytes)) != -1) {
                            servletOut.write(bytes, 0, readBytes);
                            servletOut.flush();
                        }
                        servletOut.flush();
                    } finally {
                        ctxt.getAsyncContext().complete();
                    }
                }

                @Override
                public void onError(Throwable t) {
                    // never mind
                    log.warn("Error writing output", t);
                }
            });
        } catch (Exception e) {
            log.warn("Couldn't write output", e);
        }
    }

    private void setMessageHandler(DefaultHttpRequestContext request) throws MessageException, NotFoundException {
        Object requestBean = requestToMessageMapper.mapToRequestBean(request);
        IMessageHandler handler = findRequestHandlerFor(requestBean);
        
        if(!handler.getMessageType().isAssignableFrom(requestBean.getClass())){
            throw new MessageException("Invalid mapping, request bean type " + requestBean.getClass().getName() + " is not assignable to invoker message type " + handler.getMessageType().getName());
        }
        
        request.setProperty(INVOKER_KEY, handler);
        request.setHandlerMethod(handler.getMethod());
    }

    private HttpResponse invokeMessageHandlerAndWrapResponse(HttpRequestContext httpContext) {
        HttpResponse response;
        Object result = invokeMessageHandler(httpContext);

        if (result instanceof HttpResponse) {
            response = (HttpResponse) result;
        } else { // make default response
            response = DefaultHttpResponse.ok().entity(result).build();
        }
        return response;
    }

    private Object invokeMessageHandler(HttpRequestContext request) throws MessageException, NotFoundException {
        Object requestBean = requestToMessageMapper.mapToRequestBean(request);
        IMessageHandler invoker = (IMessageHandler) request.getProperty(INVOKER_KEY);
        Object entity = invoker.invoke(request, requestBean);
        return entity;
    }

    private IMessageHandler findRequestHandlerFor(Object requestBean) throws NotFoundException {
        IMessageHandler handler = messageHandlers.getHandlerFor(requestBean.getClass());
        if (handler == null) {
            throw new NotFoundException("No handler registered for reqest bean " + requestBean.getClass().getName());
        }
        return handler;
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(requestProcessingQueue);
    }

    public static class Builder {

        private MediaType defaultMediaType;
        private Locale defaultLocale;
        private String defaultLanguage;
        private Duration asyncTimeout;

        private HttpResponseBodyWriter responseBodyWriter;
        private ErrorResponseHandler errorHandler;
        private ValueConverterFactory converterFactory;

        private List<HttpFilter> filters = new ArrayList<>();
//        private List<HttpFilter> afterMatchFilters = new ArrayList<>();

        private IRequestToMessageMapper requestToMessageMapper;
        private IMessageHandlerProvider messageHandlerProvider;
        
        public CqrsService build() {
            Preconditions.checkNotNull(defaultMediaType, "defaultMediaType");
            Preconditions.checkNotNull(defaultLocale, "defaultLocale");
            Preconditions.checkNotNull(defaultLanguage, "defaultLanguage");
            Preconditions.checkNotNull(asyncTimeout, "asyncTimeout");

            Preconditions.checkNotNull(converterFactory, "converterFactory");
            Preconditions.checkNotNull(errorHandler, "errorHandler");
            Preconditions.checkNotNull(responseBodyWriter, "responseBodyWriter");
            Preconditions.checkNotNull(requestToMessageMapper, "requestToMessageMapper");
            Preconditions.checkNotNull(messageHandlerProvider, "messageHandlerProvider");
            

            Variant defaultVariant = new Variant(defaultMediaType, defaultLocale, defaultLanguage);

            //Collections.sort(beforeMatchFilters,FILTER_PRIORITY_COMPARATOR);
            
            return new CqrsService(defaultVariant, asyncTimeout.getMillis(), messageHandlerProvider, requestToMessageMapper, responseBodyWriter, errorHandler,
                    converterFactory, filters);
        }

        public Builder defaults() {
            defaultMediaType = MediaType.APPLICATION_JSON_TYPE;
            defaultLocale = Locale.ENGLISH;
            defaultLanguage = "UTF-8";
            asyncTimeout = Duration.millis(2 * 60 * 1000);

            converterFactory = new ValueConverterFactory();
            responseBodyWriter = new DefaultResponseBodyWriter();
            errorHandler = new DefaultErrorResponseHandler();
            return this;
        }

        @Required
        public Builder requestMapper(IRequestToMessageMapper requestToMessageMapper) {
            this.requestToMessageMapper = requestToMessageMapper;
            return this;
        }
        
        @Required
        public Builder handlers(IMessageHandlerProvider provider){
            this.messageHandlerProvider = provider;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder defaultMediaType(String mediaType) {
            defaultMediaType(MediaType.valueOf(mediaType));
            return this;
        }
        
        @Optional(caveats="defaults set")
        public Builder defaultMediaType(MediaType defaultMediaType) {
            this.defaultMediaType = defaultMediaType;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder defaultLocale(String locale) {
            defaultLocale(Locale.forLanguageTag(locale));
            return this;
        }
        
        @Optional(caveats="defaults set")
        public Builder defaultLocale(Locale defaultLocale) {
            this.defaultLocale = defaultLocale;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder defaultLanguage(String defaultLanguage) {
            this.defaultLanguage = defaultLanguage;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder asyncTimeoutMs(long timeout) {
            asyncTimeout(Duration.millis(timeout));
            return this;
        }
        
        @Optional(caveats="defaults set")
        public Builder asyncTimeout(Duration asyncTimeout) {
            this.asyncTimeout = asyncTimeout;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder responseBodyWriter(HttpResponseBodyWriter responseBodyWriter) {
            this.responseBodyWriter = responseBodyWriter;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder errorHandler(ErrorResponseHandler errorHandler) {
            this.errorHandler = errorHandler;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder valueConverter(ValueConverterFactory converterFactory) {
            this.converterFactory = converterFactory;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder filters(List<HttpFilter> beforeMatchFilters) {
            this.filters = beforeMatchFilters;
            return this;
        }

        @Optional(caveats="defaults set")
        public Builder filter(HttpFilter filter) {
            this.filters.add(filter);
            return this;
        }
    }
}
