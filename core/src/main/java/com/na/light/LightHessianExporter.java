package com.na.light;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.HessianServiceExporter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by Administrator on 2017/8/4 0004.
 */
public class LightHessianExporter extends HessianServiceExporter {
    private final static Logger log = LoggerFactory.getLogger(LightHessianExporter.class);
    /**
     * 限制只有知道token的用户，才能访问该接口。
     */
    private String token;

    @Override
    protected ClassLoader getBeanClassLoader() {
        return this.getClass().getClassLoader();
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        log.info("=================--222------------------------===============");

        super.handleRequest(request, response);
    }

    public void setToken(String token) {
        this.token = token;
    }
}
