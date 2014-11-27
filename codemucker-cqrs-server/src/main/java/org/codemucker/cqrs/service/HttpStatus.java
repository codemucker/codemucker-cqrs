package org.codemucker.cqrs.service;

public enum HttpStatus {
    OK_200(200),NOT_FOUND_404(404),BAD_REQUEST_400(400),ACCESS_DENIED_401(401),ACCESS_FORBIDDEN_401(403),SERVER_ERROR_500(500);
    
    private final int code;
    
    private HttpStatus(int status){
        this.code = status;
    }
    
    public int getCode(){
        return code;
    }
}
