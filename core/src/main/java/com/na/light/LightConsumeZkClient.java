package com.na.light;

import com.na.light.util.IpUtils;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.net.SocketException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 服务消费方配置中心组件。
 * Created by sunny on 2017/8/10 0010.
 */
public class LightConsumeZkClient {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ZkClient zkClient;
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init(){
        String serverAddress = applicationContext.getEnvironment().getProperty("server.address");


        Map<String,LightConsumeFactoryBean> factoryBeanMap = applicationContext.getBeansOfType(LightConsumeFactoryBean.class);
        factoryBeanMap.forEach((key,item)->{
            try {
                LightRpcService lightRpcService = (LightRpcService) item.getServiceInterface().getAnnotation(LightRpcService.class);
                String serverUrls = applicationContext.getEnvironment().getProperty("spring.light.server."+lightRpcService.value());
                if(serverUrls==null || serverUrls.trim().length()==0){
                    configZookeeper(serverAddress, item);
                }else {
                    configDirect(item);
                }
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        });
    }

    private void configDirect(LightConsumeFactoryBean item) throws SocketException {
        LightRpcService lightRpcService = (LightRpcService) item.getServiceInterface().getAnnotation(LightRpcService.class);
        String remote = "/light-rpc/" + lightRpcService.value() + "/providers";

        String configServers = applicationContext.getEnvironment().getProperty("spring.light.server."+lightRpcService.value());
        String token = applicationContext.getEnvironment().getProperty("spring.light.server."+lightRpcService.value()+".token");

        List<String> urls = Arrays.asList(configServers.split(","));

        Map<String,LightServiceNodeData> childrenData = new HashMap<>();
        urls.forEach(url->{
            LightServiceNodeData data = new LightServiceNodeData();
            data.setServiceName(lightRpcService.value());
            data.setToken(token);
            childrenData.put(url,data);
        });
        item.freshUrl(childrenData);

        log.info("直接调用接口服务配置成功："+remote+","+configServers);
    }

    private void configZookeeper(String serverAddress, LightConsumeFactoryBean item) throws SocketException {
        LightRpcService lightRpcService = (LightRpcService) item.getServiceInterface().getAnnotation(LightRpcService.class);
        String remote = "/light-rpc/" + lightRpcService.value() + "/providers";

        String root = "/light-rpc";
        String serviceName = lightRpcService.value();
        String consume = "consumes";
        String ip = serverAddress!=null ? serverAddress : IpUtils.getRealIp();

        if(!zkClient.exists(root)) {
            zkClient.createPersistent(root);
        }
        if(!zkClient.exists(root+"/"+serviceName)){
            zkClient.createPersistent(root+"/"+serviceName);
        }
        if(!zkClient.exists(root+"/"+serviceName+"/"+consume)){
            zkClient.createPersistent(root+"/"+serviceName+"/"+consume);
        }

        zkClient.delete(root+"/"+serviceName+"/"+consume+"/"+ip);
        zkClient.createEphemeral(root+"/"+serviceName+"/"+consume+"/"+ip);

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
    }
}
