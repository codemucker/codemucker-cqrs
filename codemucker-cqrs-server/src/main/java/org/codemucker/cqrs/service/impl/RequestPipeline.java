package org.codemucker.cqrs.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.codemucker.cqrs.service.DefaultHttpRequestContext;
import org.codemucker.cqrs.service.HttpFilter;
import org.codemucker.cqrs.service.HttpResponse;

public class RequestPipeline implements PipelineHandler {

    private final PipelineFilter[] filters;
    private final PipelineHandler handler;

    public static Builder with(){
        return new Builder();
    }
    
    private RequestPipeline(PipelineFilter[] filters,PipelineHandler handler) {
        super();
        this.filters = filters;
        this.handler = handler;
    }
    
    @Override
    public HttpResponse invoke(DefaultHttpRequestContext request){
        return invokeNext(request, 0);
    }

    public HttpResponse invokeNext(DefaultHttpRequestContext request, int sequence) {
        int next = sequence + 1;
        if (filters.length > next) {
            return filters[next].onFilter(request, this);
        } else {
            return invokeHandler(request);
        }
    }

    private HttpResponse invokeHandler(DefaultHttpRequestContext request) {
        return handler.invoke(request);
    }
    
    public static class Builder {
        private List<PipelineFilter> filters  = new ArrayList<>();
        private PipelineHandler handler;
        
        public RequestPipeline build(){
            if(handler == null){
                throw new IllegalStateException("expect handler");
            }
            return new RequestPipeline(filters.toArray(new PipelineFilter[filters.size()]), handler);
        }
        
        public Builder filters(Iterable<HttpFilter> filters){
            if (filters != null) {
                for (HttpFilter filter : filters) {
                    filter(filter);
                }
            }
            return this;
        }
        
        public Builder filter(HttpFilter filter){
            filters.add(new PipelineFilter(filter, filters.size()));
            return this;
        }
        
        public Builder handler(PipelineHandler handler){
            this.handler = handler;
            return this;
        }
    }

}
