package org.codemucker.cqrs.generator.test.api;

import javax.ws.rs.Path;

@Path("/path/of/my/cmd/${myParam1}")
public class MyTestCmd {

    public String myParam1;
    private String myParam2;

    public String getMyParam2() {
        return myParam2;
    }

    public void setMyParam2(String myParam2) {
        this.myParam2 = myParam2;
    }

}
