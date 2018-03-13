package com.na.light;


import com.caucho.hessian.io.SerializerFactory;
import com.na.light.hessian.BigDecimalSerializerFactory;
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
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 服务消费者spring工厂类。
 * Created by sunny on 2017/8/1 0001.
 */
public class LightConsumeFactoryBean implements FactoryBean<Object>{
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private List<ServiceProxyEntry> proxyEntries = new ArrayList<>();
    private Class serviceInterface;
    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private Integer index = 0;

    public void addServiceUrl(String interfaceName, LightServiceNodeData data) {
        lock.writeLock().lock();
        try {
            this.proxyEntries.add(new ServiceProxyEntry(interfaceName, data));
        }finally {
            lock.writeLock().unlock();
        }
    }

    public Class getServiceInterface() {
        return serviceInterface;
    }

    /**
     * 刷新服务器列表。
     * key 服务提供方ip地址及端口号,val 该节点存储的数据。
     * @param children
     */
    public void freshUrl(Map<String,LightServiceNodeData> children){
        log.info("刷新服务器列表……");
        lock.writeLock().lock();
        try {

//            Iterator<ServiceProxyEntry> it = proxyEntries.iterator();
//            while (it.hasNext()) {
//                ServiceProxyEntry entry = it.next();
//                boolean isExist = children.keySet().stream().anyMatch(interfaceName -> {
//                    return interfaceName.equalsIgnoreCase(entry.getServiceUrl());
//                });
//                if (!isExist) {
//                    it.remove();
//                }
//            }
            proxyEntries.clear();
            children.forEach((interfaceName, data) -> {
//                boolean isExist = proxyEntries.stream().anyMatch(entity -> {
//                    return entity.getServiceUrl().equalsIgnoreCase(interfaceName);
//                });
//                if (!isExist) {
                this.addServiceUrl(interfaceName, data);
//                }
            });
            log.info("服务器清单：" + proxyEntries.toString());
        }finally {
            lock.writeLock().unlock();
        }
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

            Object result = null;
            if(subject instanceof List) {
                for(Object s : (List)subject) {
                    result = method.invoke(subject, args);
                }
            } else {
                result = method.invoke(subject, args);
            }
            LightConfig.setSelector(null);
            return result;
        }

        private Object getSubject() {
            lock.readLock().lock();
            try {
                boolean isBalance = true;
                ServiceProxyEntry proxyEntry = null;
                List<ServiceProxyEntry> proxyEntries = new ArrayList<>();
                String selector = String.valueOf(LightConfig.getThreadLocal().get());
                if("".equals(selector) || "null".equals(selector)) {
                    proxyEntries = LightConsumeFactoryBean.this.proxyEntries;
                    isBalance = true;
                } else if ("*".equals(selector)) {
                    proxyEntries = LightConsumeFactoryBean.this.proxyEntries;
                    isBalance = false;
                } else {
                    for(ServiceProxyEntry entry : LightConsumeFactoryBean.this.proxyEntries) {
                        String [] selectors = entry.data.getSelector().trim().split(",");
                        for (String s : selectors) {
                            if(selector.trim().startsWith(s.trim())) {
                                proxyEntries.add(entry);
                            }
                        }
                    }
                    isBalance = true;
                }

                if (proxyEntries.size() == 0) {
                    throw new RuntimeException("没有可用服务器……");
                }

                Object subject = null;
                if(isBalance) {
                    synchronized (index) {
                        index = index++ % proxyEntries.size();
                        proxyEntry = proxyEntries.get(index++ % proxyEntries.size());
                    }

                    subject = proxyEntry.getServiceProxy();
                    if (subject == null) {
                        subject = createBean(proxyEntry);
                        proxyEntry.setServiceProxy(subject);
                    }

                    log.info("开始调用远程接口{}...", proxyEntry.getFullUrl());
                } else {
                    subject = new ArrayList<>();
                    for (ServiceProxyEntry entry : proxyEntries) {
                        if (entry.getServiceProxy() == null) {
                            entry.setServiceProxy(createBean(entry));
                        }
                        ((ArrayList)subject).add(entry.getServiceProxy());
                        log.info("开始调用远程接口{}...", entry.getFullUrl());
                    }
                }

                return subject;
            }finally {
                lock.readLock().unlock();
            }
        }

        private Object createBean(ServiceProxyEntry proxyEntry){
            LightServiceNodeData data = proxyEntry.getData();
            LightRpcService lightRpcService = (LightRpcService)serviceInterface.getAnnotation(LightRpcService.class);
            HessianProxyFactoryBean bean = new HessianProxyFactoryBean();
            bean.setServiceInterface(serviceInterface);
            bean.setServiceUrl(proxyEntry.getFullUrl(lightRpcService));
            bean.setPassword(data.getToken());
            bean.setUsername("test");
            SerializerFactory se = SerializerFactory.createDefault();
            se.addFactory(new BigDecimalSerializerFactory());
            bean.setSerializerFactory(se);
            log.info("远程调用接口注册成功：{}",proxyEntry.getFullUrl(lightRpcService));
            bean.prepare();
            return new ProxyFactory(serviceInterface, bean).getProxy(ClassUtils.getDefaultClassLoader());
        }
    }


    private static class ServiceProxyEntry {
        /**
         * 提供服务方的IP地址+端口号。
         */
        private String serviceUrl;
        /**
         * 代理对象。
         */
        private Object serviceProxy;
        /**
         * 接口服务端的相关调用数据。
         */
        private LightServiceNodeData data;
        /**
         * 完整的调用地址。
         */
        private String fullUrl;

        public ServiceProxyEntry(String serviceUrl, LightServiceNodeData data) {
            this.serviceUrl = serviceUrl;
            this.data = data;
        }

        public String getServiceUrl() {
            return serviceUrl;
        }

        public LightServiceNodeData getData() {
            return data;
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

        public String getFullUrl() {
            return fullUrl;
        }

        public String getFullUrl(LightRpcService lightRpcService){
            StringBuilder url = new StringBuilder("http://").append(this.getServiceUrl()).append("/");
            if(data!=null && data.getContextPath()!=null){
                if(data.getContextPath().indexOf("/")==0){
                    url.append(data.getContextPath().substring(1));
                }else {
                    url.append(data.getContextPath());
                }
                url.append("/");
            }

            if(data!=null && data.getGroup()!=null){
                if(data.getGroup().indexOf("/")==0){
                    url.append(data.getGroup().substring(1));
                }else {
                    url.append(data.getGroup());
                }
                url.append("/");
            }

            if(lightRpcService.value().indexOf("/")==0){
                url.append(lightRpcService.value().substring(1));
            }else {
                url.append(lightRpcService.value());
            }
            this.fullUrl = url.toString();
            return this.fullUrl;
        }
    }

}
