package com.na.light;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.remoting.caucho.HessianServiceExporter;
import org.springframework.util.Base64Utils;

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
        if(token!=null) {
            String tempToken = getRequestToken(request);
            if(!token.equals(tempToken)){
                throw new RuntimeException("没权限访问给接口！");
            }
        }

        super.handleRequest(request, response);
    }

    private String getRequestToken(HttpServletRequest request){
        String auth = request.getHeader("authorization");
        if(auth!=null){
            auth = auth.substring(6);
            auth = new String(Base64Utils.decodeFromString(auth));
            String[] auths =  auth.split(":");
            if(auths!=null && auths.length==2){
                return auths[1];
            }
        }
        return null;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
