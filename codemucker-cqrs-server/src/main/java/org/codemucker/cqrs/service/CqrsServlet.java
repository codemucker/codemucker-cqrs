package org.codemucker.cqrs.service;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.inject.Inject;

public class CqrsServlet implements Servlet {

    private final Logger log = LogManager.getLogger(CqrsServlet.class);
    
    private final CqrsService service;
    
    @Inject
    public CqrsServlet(CqrsService service) {
        super();
        this.service = service;
    }

    @Override
    public ServletConfig getServletConfig() {
        return null;
    }

    @Override
    public String getServletInfo() {
        return "CQRS Servlet";
    }

    @Override
    public void init(ServletConfig cfg) throws ServletException {
    }

    @Override
    public void service(ServletRequest request, ServletResponse response) throws ServletException, IOException {
       try {
           service.onRequest((HttpServletRequest)request, (HttpServletResponse)response);
       } catch (Exception e){
           log.error("unhandled error while processing request", e);
       }
    }

    @Override
    public void destroy() {
        IOUtils.closeQuietly(service);
    }

}
