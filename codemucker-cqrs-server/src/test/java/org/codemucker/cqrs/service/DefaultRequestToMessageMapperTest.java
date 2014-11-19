package org.codemucker.cqrs.service;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.codemucker.cqrs.POST;
import org.codemucker.cqrs.Path;
import org.codemucker.cqrs.service.json.JacksonRequestBodyReader;
import org.codemucker.cqrs.service.mock.MockHttpRequest;
import org.codemucker.jmatch.Expect;
import org.junit.Test;

public class DefaultRequestToMessageMapperTest {

    @Test
    public void testBeanIsMapped() throws Exception {
        
        JacksonRequestBodyReader jsonReader = new JacksonRequestBodyReader(new DefaultJacksonConfig().getMapper());
        ValueConverterFactory converters = new ValueConverterFactory();
        ConfigurableResponseBodyReader bodyReader = ConfigurableResponseBodyReader.with()
                .defaults()
                .reader(MediaType.APPLICATION_JSON_TYPE, jsonReader)
                .build();
        
        DefaultRequestToMessageMapper mapper = DefaultRequestToMessageMapper.with()
                .valueConverter(converters)
                .bodyReader(bodyReader)
                .messageBean(TestCmdBean.class)
                .build();
        
        MockHttpRequest req = new MockHttpRequest();
        req.setMediaType(MediaType.APPLICATION_JSON_TYPE);
        req.setHttpMethod("POST");
        req.setPath("/path/to/my/p1value/bean?p4=foo&p5=yes&p6=f");
        req.mockSetBody("{param2:1234,'param3':'true'}");
        req.mockAddHeader("h1","h1value");
        req.mockAddCookie("c1","c1value");
        req.mockAddCookie("param9","p9value");
        
        
        TestCmdBean bean = (TestCmdBean)mapper.mapToRequestBean(req);
        
        Expect.that(bean).isNotNull();
        Expect.that(bean.param1).isEqualTo("p1value");
        Expect.that(bean.param2).isEqualTo(1234);
        Expect.that(bean.param3).isEqualTo(true);
        Expect.that(bean.param4).isEqualTo("foo");
        Expect.that(bean.param5).isEqualTo(true);
        Expect.that(bean.param6).isEqualTo(false);
        Expect.that(bean.param7).isEqualTo("h1value");
        Expect.that(bean.param8).isEqualTo("c1value");
        Expect.that(bean.param9).isEqualTo("p9value");
        
        MockHttpRequest req2 = new MockHttpRequest();
        req2.setMediaType(MediaType.APPLICATION_JSON_TYPE);
        req2.setHttpMethod("POST");
        req2.setPath("/otherpath/to/my/5678/bean?param2=1234");//query ignored
        req2.mockSetBody("{param2:1234}");//overridden by path
        TestCmdBean bean2 = (TestCmdBean)mapper.mapToRequestBean(req2);
        Expect.that(bean2.param2).isEqualTo(5678);
    }

    
    @Path({"/path/to/my/${name}/bean","/otherpath/to/my/${param2}/bean"})
    @POST
    static class TestCmdBean {

        @PathParam("name")
        private String param1;
        //unmarked param
        private int param2;
        private boolean param3;
        @QueryParam("p4")
        public String param4;
        @QueryParam("p5")
        public Boolean param5;
        @QueryParam("p6")
        public Boolean param6;
        @HeaderParam("h1")
        public String param7;
        @CookieParam("c1")
        public String param8;
        @CookieParam("")
        public String param9;

        public String getParam1() {
            return param1;
        }

        public void setParam1(String param1) {
            this.param1 = param1;
        }

        public int getParam2() {
            return param2;
        }

        public void setParam2(int param2) {
            this.param2 = param2;
        }

        public boolean isParam3() {
            return param3;
        }

        public void setParam3(boolean param3) {
            this.param3 = param3;
        }

    }

}
