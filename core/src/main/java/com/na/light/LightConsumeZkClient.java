package com.na.light;

import com.na.light.hessian.LightProxyFactoryBean;
import com.na.light.util.IpUtils;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务消费方配置中心组件。
 * Created by sunny on 2017/8/10 0010.
 */
@Component
public class LightConsumeZkClient {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ZkClient zkClient;
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init(){
        String zookeeperUrl = applicationContext.getEnvironment().getProperty("spring.light.zookeeper.url");
        String contextPath = applicationContext.getEnvironment().getProperty("server.context-path");
        //单位：毫秒
        Integer zookeeperTimeout = applicationContext.getEnvironment().getProperty("spring.light.zookeeper.timeout",Integer.class,30*1000);

        Map<String,LightProxyFactoryBean> factoryBeanMap = applicationContext.getBeansOfType(LightProxyFactoryBean.class);
        factoryBeanMap.forEach((key,item)->{
            try {
                LightRpcService lightRpcService = (LightRpcService) item.getServiceInterface().getAnnotation(LightRpcService.class);
                String remote = "/light-rpc/" + lightRpcService.value() + "/providers";
                if(zkClient.exists(remote)) {
                    List<String> urls = zkClient.getChildren(remote);
                    Map<String,LightServiceNodeData> children = new HashMap<>();
                    urls.forEach(url->{
                        LightServiceNodeData data = zkClient.readData(remote+"/"+url);
                        children.put(url,data);
                    });
                    item.freshUrl(children);
                }
                zkClient.subscribeChildChanges(remote, ((parentPath, currentChilds) -> {
                    if(remote.equals(parentPath) && currentChilds!=null){
                        Map<String,LightServiceNodeData> children = new HashMap<>();
                        currentChilds.forEach(interfaceName->{
                            LightServiceNodeData data = zkClient.readData(remote+"/"+interfaceName);
                            children.put(interfaceName,data);
                        });
                        item.freshUrl(children);
                    }
                }));
                log.info("向配置中心注册服务消费方成功");
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        });
    }
}
