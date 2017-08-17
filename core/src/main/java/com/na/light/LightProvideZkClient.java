package com.na.light;

import com.na.light.util.IpUtils;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.util.Map;

/**
 * 服务提供方向配置中心注册组件。
 * Created by sunny on 2017/8/10 0010.
 */
public class LightProvideZkClient {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ZkClient zkClient;
    @Autowired
    private ApplicationContext applicationContext;

    @PostConstruct
    public void init(){
        int port = applicationContext.getEnvironment().getProperty("server.port",int.class);
        String serverAddress = applicationContext.getEnvironment().getProperty("server.address");
        String contextPath = applicationContext.getEnvironment().getProperty("server.context-path");

        Map<String,LightHessianExporter> factoryBeanMap = applicationContext.getBeansOfType(LightHessianExporter.class);
        factoryBeanMap.forEach((key,item)->{
            try {
                LightRpcService lightRpcClient = (LightRpcService) item.getOriginalServiceCls().getAnnotation(LightRpcService.class);
                String root = "/light-rpc";
                String serviceName = lightRpcClient.value().trim().length()==0 ? item.getServiceInterface().getSimpleName() : lightRpcClient.value();
                String providers = "providers";

                if(!zkClient.exists(root)) {
                    zkClient.createPersistent(root);
                }
                if(!zkClient.exists(root+"/"+serviceName)){
                    zkClient.createPersistent(root+"/"+serviceName);
                }
                if(!zkClient.exists(root+"/"+serviceName+"/"+providers)){
                    zkClient.createPersistent(root+"/"+serviceName+"/"+providers);
                }

                String ip = serverAddress!=null?serverAddress : IpUtils.getRealIp();
                LightServiceNodeData data = new LightServiceNodeData();
                data.setContextPath(lightRpcClient.group());
                data.setVersion(1);
                data.setGroup(lightRpcClient.group());
                data.setContextPath(contextPath);
                data.setToken(item.getToken());
                zkClient.createEphemeral(root+"/"+serviceName+"/"+providers+"/"+ip+":"+port,data);
                log.info("向配置中心注册服务提供方成功");
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        });
    }
}
