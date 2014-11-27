package org.codemucker.cqrs.client.gwt;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ValidationException;
import javax.validation.Validator;

import org.codemucker.cqrs.MessageException;
import org.codemucker.cqrs.client.CqrsRequestHandle;
import org.codemucker.lang.IBuilder;

import com.github.nmorel.gwtjackson.client.ObjectReader;
import com.google.gwt.core.client.Callback;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestBuilder.Method;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.Response;

public abstract class AbstractCqrsGwtClient {
    private String baseUrl;

    protected AbstractCqrsGwtClient(String baseUrl){
        this.baseUrl = baseUrl;
    }
    
    protected Builder newRequestBuilder() {
        Builder b = new Builder(baseUrl);// new Request();
        
        return b;
    }

    protected void checkForValidationErrors(Set<? extends ConstraintViolation<?>> violations, String requestName) {
        if(violations != null && violations.size() > 0){
            throw new ConstraintViolationException("Invalid " + requestName + ", constraints not met",(Set<ConstraintViolation<?>>) violations);
        }
    }
    
    protected abstract Validator getValidator();
    
    protected boolean isFieldSet(String value) {
        return value != null && value.trim().length() > 0;
    }

    protected boolean isFieldSet(Object value) {
        return value != null;
    }
    
    protected ValidationException newInvalidRequestFieldsFor(String name){
        return new ValidationException("Invalid fields for request " + name);
    }

    protected <TRequest,TResponse> CqrsRequestHandle invokeAsync(final TRequest request, final Builder b,final ObjectReader<TResponse> jsonReader,final Callback<TResponse, MessageException> callback) {
        if (b == null) {
            throw new NullPointerException("request builer is null");
        }
        final CqrsRequest cqrsRequest = b.build();
   
        //    LOG.fine("http call:" + httpRequest.getHTTPMethod() + " " + httpRequest.getUrl() + "  content-type:" + httpRequest.getHeader("Content-Type"));
        
        try {
            cqrsRequest.builder.setCallback(new RequestCallback() { 
                @Override
                public void onResponseReceived(Request request, Response response) {
                    int status  = response.getStatusCode();
                    if(!cqrsRequest.getExpectStatusCodes().contains(status)){
                        String msg = "invalid response status "  + status + " for " + cqrsRequest.builder.getHTTPMethod() + ":" + cqrsRequest.builder.getUrl() + ". Expected one of [" + Arrays.toString(cqrsRequest.getExpectStatusCodes().toArray()) + "]";
                        callback.onFailure(new MessageException(msg));
                        return;
                    }
                    String body = response.getText();
                    //TODO:check if error response type
                    
                    TResponse res;
                    try{
                       res= jsonReader.read(body);
                    } catch(Exception e){
                        String msg = "error converting response from " + cqrsRequest.builder.getHTTPMethod() + ":" + cqrsRequest.builder.getUrl() + " to json. status:"  + response.getStatusText() + ", body:" + body;
                        callback.onFailure(new MessageException(msg,e));
                        return;
                    }
                    callback.onSuccess(res);
                }
                
                @Override
                public void onError(Request request, Throwable e) {
                    //LOG.warning(ErrorUtils.toLogMessage("request error", e));
                    callback.onFailure(new MessageException("request error",e));
                }
            });
        
            return new DefaultApiRequestHandle(cqrsRequest.builder.send());
        } catch (Exception e) {
            callback.onFailure(new MessageException("something wrong with request", e));
            return new FailedApiRequestHandle();
        }
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private static class DefaultApiRequestHandle implements CqrsRequestHandle {

        private final Request httpRequest;
        
        DefaultApiRequestHandle(Request httpRequest){
            this.httpRequest = httpRequest;
        }
        @Override
        public void cancel() {
            httpRequest.cancel();
        }

        @Override
        public boolean isPending() {
            return httpRequest.isPending();
        }
    }
    
    private static class FailedApiRequestHandle implements CqrsRequestHandle {
        @Override
        public void cancel() {
            //no op
        }

        @Override
        public boolean isPending() {
            return false;
        }
    }
    
    public static class CqrsRequest {
        final RequestBuilder builder;
        final List<Integer> expectStatus;
        
        public CqrsRequest(RequestBuilder builder, List<Integer> expectStatus) {
            super();
            this.builder = builder;
            this.expectStatus = expectStatus;
        }
        
        public List<Integer> getExpectStatusCodes(){
            return expectStatus;
        }
    }
    
    public static class Builder implements IBuilder<CqrsRequest> {
        private String url;
        private Method method = RequestBuilder.GET;
        private int timeoutMs;
        private Object bodyJson;
        private String bodyRaw;
        private String contentType;
        private String acceptContentType;
        private List<Integer> expectStatus = new ArrayList<>();
        
        private HashMap<String, Object> headers = new HashMap<String, Object>();
        private HashMap<String, Object> queryParams = new HashMap<String, Object>();
        private HashMap<String, Object> bodyParams = new HashMap<String, Object>();

        public Builder(String baseUrl) {
            this.url = baseUrl;
        }

        @Override
        public CqrsRequest build() {
            RequestBuilder builder = new RequestBuilder(method, url);
            builder.setTimeoutMillis(timeoutMs);
            builder.setHeader("Content-Type", contentType);
            builder.setHeader("Accept", acceptContentType);
            builder.setHeader("Accept-Charset", "utf-8");
            if (bodyRaw != null) {
                builder.setRequestData(bodyRaw);
            } else if (bodyJson != null) {
                String requestDataAsJson = null;//TODO: encoder.write(bodyJson);
                builder.setRequestData(requestDataAsJson);
            }
            if(expectStatus.isEmpty()){
                expectStatus.add(200);
            }
            return new CqrsRequest(builder,expectStatus);
        }

        public Builder setHeaderIfNotNull(String name, Object value) {
            if (value != null) {
                headers.put(name, value);
            }
            return this;
        }

        public Builder setQueryParamIfNotNull(String name, Object value) {
            if (value != null) {
                queryParams.put(name, value);
            }
            return this;
        }

        public Builder setBodyParamIfNotNull(String name, Object value) {
            if (value != null) {
                bodyParams.put(name, value);
            }
            return this;
        }

        public Builder method(String method) {
            method = method.toLowerCase();
            if ("get".equals(method)) {
                methodGET();
            } else if ("post".equals(method)) {
                methodPOST();
            } else if ("put".equals(method)) {
                methodPUT();
            } else if ("head".equals(method)) {
                methodHEAD();
            } else if ("delete".equals(method)) {
                methodDELETE();
            } else {
                throw new IllegalArgumentException("method '" + method + "' not understood");
            }
            return this;
        }

        public Builder addQueryParam(String name, Object val) {
            queryParams.put(name, val);
            return this;
        }

        public Builder addPathPartIfNotBlank(String path) {
            if (path != null && path.length() > 0) {
                addPathPart(path);
            }
            return this;
        }

        public Builder addPathPart(String path) {
          //escape naughty values
            try {
                path = URLEncoder.encode(path,"UTF-8");
                addSafePathPart(path);
            } catch (UnsupportedEncodingException e) {
                //never thrown
            }
            return this;
        }

        public Builder addPathPart(Enum<?> enumPart) {
            addSafePathPart(enumPart.name());
            return this;
        }
        
        public Builder addSafePathPart(String path) {
            if (path.startsWith("/")) {
                path = path.substring(1, path.length());
            }
            if (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            
            if (this.url == null) {
                this.url = path;
            } else {
                if (!this.url.endsWith("/")) {
                    this.url += "/";
                }
                this.url += path;
            }
            return this;
        }

        public Builder addCookie(String name, String value) {
            // TODO:add cookie
            return this;
        }

        public Builder addHeader(String name, String value) {
            headers.put(name, value);
            return this;
        }

        public Builder bodyRaw(String data) {
            clearBody();
            this.bodyRaw = data;
            return this;
        }

        public Builder bodyAsJson(Object pojo) {
            clearBody();
            this.bodyJson = pojo;
            return this;
        }

        private void clearBody() {
            this.bodyParams.clear();
            this.bodyJson = null;
            this.bodyRaw = null;
        }

        public Builder expectStatus(int status) {
            expectStatus.add(status);
            return this;
        }

        public Builder expectStatus(int status, int orStatus) {
            expectStatus.add(status);
            expectStatus.add(orStatus);
            return this;
        }

        public Builder expectStatus(int... status) {
            for(int val:status){
                expectStatus.add(val);
            }
            return this;
        }

        public Builder setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
            return this;
        }

        public Builder methodPOST() {
            this.method = RequestBuilder.POST;
            return this;
        }

        public Builder methodGET() {
            this.method = RequestBuilder.GET;
            return this;
        }

        public Builder methodPUT() {
            this.method = RequestBuilder.PUT;
            return this;
        }

        public Builder methodDELETE() {
            this.method = RequestBuilder.DELETE;
            return this;
        }

        public Builder methodHEAD() {
            this.method = RequestBuilder.HEAD;
            return this;
        }

        public Builder requestContentType(String type) {
            this.contentType = type;
            return this;
        }

        public Builder acceptContentType(String type) {
            this.acceptContentType = type;
            return this;
        }
    }
}
