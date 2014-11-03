package org.codemucker.cqrs.generator.test.api;

import javax.ws.rs.QueryParam;

import org.codemucker.cqrs.RestPath;

@RestPath("/path/of/my/quey/${myParam1}")
public class MyTestQuery {

    public String myParam1;
    
    @QueryParam("q")
    private String myParam2;

    public String getMyParam2() {
        return myParam2;
    }

    public void setMyParam2(String myParam2) {
        this.myParam2 = myParam2;
    }
    
    
}
