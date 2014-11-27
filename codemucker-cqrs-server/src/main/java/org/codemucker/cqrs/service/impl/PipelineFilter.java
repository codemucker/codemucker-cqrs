package org.codemucker.cqrs.service.impl;

import org.codemucker.cqrs.service.DefaultHttpRequestContext;
import org.codemucker.cqrs.service.HttpFilter;
import org.codemucker.cqrs.service.HttpResponse;

class PipelineFilter {

    private final HttpFilter filter;
    private final int sequence;

    public PipelineFilter(HttpFilter filter,int sequence) {
        super();
        this.filter = filter;
        this.sequence = sequence;
    }
    
    public HttpResponse onFilter(DefaultHttpRequestContext request,RequestPipeline pipeline){
        HttpResponse response = null;
        try {
            filter.onRequest(request);
            if(request.isAborted()){
               return request.getResponse(); 
            }
            response =  pipeline.invokeNext(request,sequence);
        } catch (Throwable t){
            filter.onError(request,t);
            if(request.isAborted()){
                response = request.getResponse(); 
             }
        } finally {
            filter.onResponse(request, response);
            if(request.isAborted()){
                response = request.getResponse(); 
             }
        }
        return response;
    }
}