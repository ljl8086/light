package com.na.light;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Created by sunny on 2017/8/3 0003.
 */
@Deprecated
public class LightProvideFactoryBean implements FactoryBean<Object>  {
    private Class serviceInterface;
    /**
     * 限制只有知道token的用户，才能访问该接口。
     */
    private String token;

    @Override
    public Object getObject() throws Exception {
        return serviceInterface.newInstance();
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    public Class getServiceInterface() {
        return serviceInterface;
    }

    public void setServiceInterface(Class serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        if(token!=null && token.trim().length()>=0){
            this.token = token;
        }
    }
}
