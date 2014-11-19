package org.codemucker.cqrs.service;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.ws.rs.Path;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.codemucker.cqrs.service.json.JacksonRequestBodyReader;
import org.codemucker.jmatch.AString;
import org.codemucker.jmatch.Expect;
import org.codemucker.testserver.AHttpEntity;
import org.codemucker.testserver.AHttpResponse;
import org.codemucker.testserver.TestServer;
import org.junit.After;
import org.junit.Test;

public class CqrsServiceTest {
    
    TestServer server = new TestServer();

    @After
    public void tearDown(){
        server.stop();
    }
    
    private void service(Builder builder) throws Exception{
        CqrsService service = builder.build();
        
        server.addServlet("/", new CqrsServlet(service));
        server.start();
    }
    
    private Builder serviceWith(){
        return new Builder();
    }
    
    private HttpGet get(String relPath){
        return new HttpGet(url(relPath));
    }
    
    private String url(String relPath){
        return "http://" + server.getHost() + ":" + server.getHttpPort() + relPath;
    }
    
    private HttpResponse execute(HttpUriRequest request) throws Exception{
        HttpClient client = HttpClientBuilder.create().build();

        return client.execute(request);
    }
    
    @Test
    public void smokeTest() throws Exception{
        service(serviceWith().handler(new MyTestBeanCmdHander()));

        HttpResponse resp = execute(get("/api/my/cmd"));
        Expect.that(resp).is(AHttpResponse.with()
              //  .statusCode(200)
                .entity(AHttpEntity.with()
                        .contentAsString(AString.equalToIgnoreCaseWhiteSpace("{'result':'foobar'}"))));
    }
    
    @Path("/api/my/cmd")
    public static class MyTestBeanCmd {
        public String myParam;
     
        public static class Response {
            public String result;
        }
    }
    
    @Path("/api/my/query")
    public static class MyTestBeanQuery {
        public static class Response {
            public String result;
        }
    }
    
    static class MyTestBeanCmdHander {
        
        @MessageHandler
        public MyTestBeanCmd.Response handle(MyTestBeanCmd cmd){
            MyTestBeanCmd.Response response = new MyTestBeanCmd.Response();
            response.result = cmd.myParam + "-foobar";
            return response;
        }
    }
    
    static class Builder {
        
        private DefaultMessageHandlerProvider.Builder handlerBuilder = DefaultMessageHandlerProvider.with();
        private DefaultRequestToMessageMapper.Builder mapperBuilder = DefaultRequestToMessageMapper.with();
        private ConfigurableResponseBodyReader.Builder bodyReaderBuilder;
        private ErrorResponseHandler errorHandler = new TestErrorHandler();
        
        public static Builder with(){
            return new Builder();
        }
        
        public Builder(){
            bodyReaderBuilder = ConfigurableResponseBodyReader.with().defaults();
            bodyReaderBuilder.defaultReader(new JacksonRequestBodyReader(new DefaultJacksonConfig().getMapper()));
        }
        
        public CqrsService build(){
            ConfigurableResponseBodyReader bodyReader = bodyReaderBuilder.build();
            ValueConverterFactory converter = new ValueConverterFactory();
            
            mapperBuilder.bodyReader(bodyReader);
            mapperBuilder.valueConverter(converter);
            
            CqrsService service = CqrsService.with()
                .defaults()
                .valueConverter(converter)
                .handlers(handlerBuilder.build())
                .requestMapper(mapperBuilder.build())
                .errorHandler(errorHandler)
                .build();
            return service;
        }
        
        public Builder handler(Object handler){
            for(IMessageHandler h : DefaultMessageHandler.createMessageHandlers(handler)){
                mapperBuilder.messageBean(h.getMessageType());
                handlerBuilder.handler(h);
            }
            return this;
        }
        
        public Builder errorHandler(ErrorResponseHandler errorHandler){
            this.errorHandler = errorHandler;
            return this;
        }
    }
    
    static class TestErrorHandler implements ErrorResponseHandler {

        @Override
        public org.codemucker.cqrs.service.HttpResponse toResponse(HttpRequest request, Throwable t) {
            System.err.println(t.toString());
            
            //t.printStackTrace(System.err);
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            pw.println(t.toString());
            //t.printStackTrace(pw);
            
            return DefaultHttpResponse.status(HttpStatus.SERVER_ERROR_500).entity(sw.toString()).build();
        }
        
    }
}
