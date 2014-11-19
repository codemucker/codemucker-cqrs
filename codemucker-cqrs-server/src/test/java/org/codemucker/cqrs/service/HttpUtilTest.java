package org.codemucker.cqrs.service;

import javax.ws.rs.core.MultivaluedHashMap;

import org.codemucker.jmatch.AList;
import org.codemucker.jmatch.Expect;
import org.junit.Test;

public class HttpUtilTest {

    @Test
    public void testParseQueryString(){
        MultivaluedHashMap<String, String> query = HttpUtil.parseQueyString("p1=v1&p2=v2a&p3=&p4=t&p2=v2b");
        Expect.that(query.get("p1")).is(AList.withOnly("v1"));
        Expect.that(query.get("p2")).is(AList.inOrder().withOnly("v2a").and("v2b"));
        Expect.that(query.get("p3")).is(AList.of(String.class).withNothing());
        Expect.that(query.get("p4")).is(AList.withOnly("t"));
    }
}
