package com.na.light;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.cglib.proxy.InvocationHandler;
import org.springframework.cglib.proxy.Proxy;
import org.springframework.remoting.caucho.HessianProxyFactoryBean;
import org.springframework.util.ClassUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Created by sunny on 2017/8/1 0001.
 */
public class LightProxyFactoryBean implements FactoryBean<Object>{
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private List<ServiceProxyEntry> proxyEntries = new ArrayList<>();
    private Class serviceInterface;

    private int index = 0;

    public void addServiceUrl(String serviceUrl) {
        this.proxyEntries.add(new ServiceProxyEntry(serviceUrl));
    }

    public Class getServiceInterface() {
        return serviceInterface;
    }

    public void freshUrl(List<String> urls){
        log.info("刷新服务器列表……");
        Iterator<ServiceProxyEntry> it = proxyEntries.iterator();
        while (it.hasNext()) {
            ServiceProxyEntry entry = it.next();
            boolean isExist = urls.stream().anyMatch(url->{return url.equalsIgnoreCase(entry.getServiceUrl());});
            if(!isExist){
                it.remove();
            }
        }
        for (String url : urls){
            boolean isExist = proxyEntries.stream().anyMatch(entity->{return entity.getServiceUrl().equalsIgnoreCase(url);});
            if(!isExist){
                this.addServiceUrl(url);
            }
        }
        log.info("服务器清单："+proxyEntries.toString());
    }

    @Override
    public Object getObject() {
        ProxyHandler proxyHandler = new ProxyHandler();
        return Proxy.newProxyInstance(serviceInterface.getClassLoader(),new Class[]{serviceInterface},proxyHandler);
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

    public void setServiceInterface(Class serviceInterface) {
        this.serviceInterface = serviceInterface;
    }

    class ProxyHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object subject = getSubject();

            System.out.println("====before====");
            Object result = method.invoke(subject, args);
            System.out.println("====after====");
            return result;
        }

        private synchronized Object getSubject() {
            ServiceProxyEntry proxyEntry = LightProxyFactoryBean.this.proxyEntries.get(index++% proxyEntries.size());
            Object subject = proxyEntry.getServiceProxy();
            if(subject==null){
                subject = createBean(proxyEntry.getServiceUrl());
                proxyEntry.setServiceProxy(subject);
            }
            return subject;
        }

        private Object createBean(String serviceUrl){
            HessianProxyFactoryBean bean = new HessianProxyFactoryBean();
            bean.setServiceInterface(serviceInterface);
            bean.setServiceUrl("http://"+serviceUrl+"/userRemote");
            bean.prepare();
            return new ProxyFactory(serviceInterface, bean).getProxy(ClassUtils.getDefaultClassLoader());
        }
    }


    private static class ServiceProxyEntry {
        private String serviceUrl;
        private Object serviceProxy;

        public ServiceProxyEntry(String serviceUrl) {
            this.serviceUrl = serviceUrl;
        }

        public String getServiceUrl() {
            return serviceUrl;
        }

        public Object getServiceProxy() {
            return serviceProxy;
        }

        public void setServiceProxy(Object serviceProxy) {
            this.serviceProxy = serviceProxy;
        }

        @Override
        public String toString() {
            return "ServiceProxyEntry{" +
                    "serviceUrl='" + serviceUrl + '\'' +
                    '}';
        }
    }

}
