package com.na.light;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.context.support.GenericWebApplicationContext;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Created by sunny on 2017/8/2 0002.
 */
@Configuration
@EnableConfigurationProperties()
public class LightRpcServiceAutoConfiguration implements ApplicationContextAware,BeanFactoryPostProcessor,BeanPostProcessor{
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private ApplicationContext applicationContext;

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
    private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

//    @Bean
//    public BeanFactoryPostProcessor beanFactoryPostProcessor() {
//        return new BeanFactoryPostProcessor() {
//            @Override
//            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
//                Scanner scanner = new Scanner((BeanDefinitionRegistry) beanFactory);
//                scanner.setResourceLoader(applicationContext);
//                String scan = applicationContext.getEnvironment().getProperty("spring.light.scan");
//                if(scan!=null && scan.trim().length()>0) {
//                    scanner.scan(scan.split(","));
//                }
//
////                beanFactory.
////                GenericWebApplicationContext context = (GenericWebApplicationContext)applicationContext;
////                registerBean(applicationContext,"zkClient",ZkClient.class,null,null);
//            }
//        };
//    }

    @Bean
    public ZkClient zkClient(){
        String zookeeperUrl = applicationContext.getEnvironment().getProperty("spring.light.zookeeper.url");
        //单位：毫秒
        Integer zookeeperTimeout = applicationContext.getEnvironment().getProperty("spring.light.zookeeper.timeout",Integer.class,30*1000);

        int port = applicationContext.getEnvironment().getProperty("server.port",int.class);
        String serverAddress = applicationContext.getEnvironment().getProperty("server.address");
        String contextPath = applicationContext.getEnvironment().getProperty("server.context-path");

        ZkClient client = getZkClient(zookeeperUrl,zookeeperTimeout);

        Map<String,RemoteServiceFactoryBean> factoryBeanMap = applicationContext.getBeansOfType(RemoteServiceFactoryBean.class);
        factoryBeanMap.forEach((key,item)->{
            try {
                LightRpcService lightRpcClient = (LightRpcService) item.getServiceInterface().getAnnotation(LightRpcService.class);
                String root = "/light-rpc";
                String serviceName = lightRpcClient.value().trim().length()==0 ? item.getServiceInterface().getSimpleName() : lightRpcClient.value();
                String providers = "providers";


                if(!client.exists(root)) {
                    client.createPersistent(root);
                }
                if(!client.exists(root+"/"+serviceName)){
                    client.createPersistent(root+"/"+serviceName);
                }
                if(!client.exists(root+"/"+serviceName+"/"+providers)){
                    client.createPersistent(root+"/"+serviceName+"/"+providers);
                }

                String ip = serverAddress!=null?serverAddress : getRealIp();
                LightServiceNodeData data = new LightServiceNodeData();
                data.setContextPath(lightRpcClient.group());
                data.setVersion(1);
                data.setGroup(lightRpcClient.group());
                data.setContextPath(contextPath);
                data.setToken(item.getToken());
                client.createEphemeral(root+"/"+serviceName+"/"+providers+"/"+ip+":"+port,data);

            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        });
        return client;
    }

    private ZkClient getZkClient(String zookeeperUrl, Integer zookeeperTimeout) {
        ZkClient client = null;
        try {
            client = applicationContext.getBean(ZkClient.class);
        }catch (Exception e){
            client = new ZkClient(zookeeperUrl,zookeeperTimeout);
        }
        return client;
    }

    public static String getRealIp() throws SocketException {
        String localip = null;// 本地IP，如果没有配置外网IP则返回它
        String netip = null;// 外网IP

        Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
        InetAddress ip = null;
        boolean finded = false;// 是否找到外网IP
        while (netInterfaces.hasMoreElements() && !finded) {
            NetworkInterface ni = netInterfaces.nextElement();
            Enumeration<InetAddress> address = ni.getInetAddresses();
            while (address.hasMoreElements()) {
                ip = address.nextElement();
                if (!ip.isSiteLocalAddress()
                        && !ip.isLoopbackAddress()
                        && ip.getHostAddress().indexOf(":") == -1) {// 外网IP
                    netip = ip.getHostAddress();
                    finded = true;
                    break;
                } else if (ip.isSiteLocalAddress()
                        && !ip.isLoopbackAddress()
                        && ip.getHostAddress().indexOf(":") == -1) {// 内网IP
                    localip = ip.getHostAddress();
                }
            }
        }

        if (netip != null && !"".equals(netip)) {
            return netip;
        } else {
            return localip;
        }
    }

    private void registerBean(ApplicationContext applicationContext, String name, Class<?> beanClass,Object obj,String token){
        ConfigurableApplicationContext context = (ConfigurableApplicationContext)applicationContext;
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry)context.getBeanFactory();

        AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);

        ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
        abd.setScope(scopeMetadata.getScopeName());

        if(obj!=null) {
            MutablePropertyValues propertyValues = new MutablePropertyValues();
            propertyValues.add("service", obj);
            propertyValues.add("token",token);
            propertyValues.add("serviceInterface", obj.getClass().getInterfaces()[0]);
            abd.setPropertyValues(propertyValues);
        }
        // 可以自动生成name
        String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, registry));

        AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);

        BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
        BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Scanner scanner = new Scanner((BeanDefinitionRegistry) beanFactory);
        scanner.setResourceLoader(applicationContext);
        String scan = applicationContext.getEnvironment().getProperty("spring.light.scan");
        if(scan!=null && scan.trim().length()>0) {
            scanner.scan(scan.split(","));
        }
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("-----------------postProcessBeforeInitialization------------------------");
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("---------------postProcessAfterInitialization--------------------------");
        return null;
    }


    public final class Scanner extends ClassPathBeanDefinitionScanner {
        public Scanner(BeanDefinitionRegistry registry) {
            super(registry);
        }

        public void registerDefaultFilters() {
            this.addIncludeFilter(new AnnotationTypeFilter(LightRpcService.class));
        }

        public Set<BeanDefinitionHolder> doScan(String... basePackages) {
            Set<BeanDefinitionHolder> beanDefinitions =   super.doScan(basePackages);
            for (BeanDefinitionHolder holder : beanDefinitions) {
                GenericBeanDefinition definition = (GenericBeanDefinition) holder.getBeanDefinition();
                String token = UUID.randomUUID().toString().replace("-","");
                Class remoteCls = null;
                try {
                    remoteCls = Class.forName(definition.getBeanClassName());
                    definition.getPropertyValues().add("serviceInterface", remoteCls);
                    definition.getPropertyValues().add("token",token);
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(),e);
                }

                LightRpcService rpcService = (LightRpcService)remoteCls.getAnnotation(LightRpcService.class);
                String remoteName = rpcService.value().trim()=="" ? remoteCls.getName() : rpcService.value();
                String group = rpcService.group().trim()=="" ? "" : "/"+rpcService.group();

                definition.setBeanClass(RemoteServiceFactoryBean.class);

                Object remoteObj = applicationContext.getBean(remoteCls);
                registerBean(applicationContext,group+"/"+remoteName, LightHessianExporter.class,remoteObj,token);

                log.info("RPC接口注册成功1[{}]",remoteName);
            }
            return beanDefinitions;
        }

        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            AnnotationMetadata metadata = beanDefinition.getMetadata();
            return  metadata.isIndependent();
        }
    }
}
