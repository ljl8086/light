package com.na.light;

import org.springframework.beans.factory.FactoryBean;

/**
 * Created by sunny on 2017/8/3 0003.
 */
public class RemoteServiceFactoryBean implements FactoryBean<Object> {
    private Class serviceInterface;

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
}
