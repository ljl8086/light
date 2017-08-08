package com.na.light;

import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sunny on 2017/8/2 0002.
 */
@Configuration
@EnableConfigurationProperties()
public class LightRpcClientAutoConfiguration implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(LightRpcClientAutoConfiguration.class);
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Bean
    public BeanFactoryPostProcessor beanFactoryPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                Scanner scanner = new Scanner((BeanDefinitionRegistry) beanFactory);
                scanner.setResourceLoader(applicationContext);
                String scan = applicationContext.getEnvironment().getProperty("spring.light.scan");
                scanner.scan(scan);
            }
        };
    }

    @Bean
    public ZkClient zkClient(){
        String zookeeperUrl = applicationContext.getEnvironment().getProperty("spring.light.zookeeper.url");
        String contextPath = applicationContext.getEnvironment().getProperty("server.context-path");
        //单位：毫秒
        Integer zookeeperTimeout = applicationContext.getEnvironment().getProperty("spring.light.zookeeper.timeout",Integer.class,30*1000);

        ZkClient client = new ZkClient(zookeeperUrl,zookeeperTimeout);

        Map<String,LightProxyFactoryBean> factoryBeanMap = applicationContext.getBeansOfType(LightProxyFactoryBean.class);
        factoryBeanMap.forEach((key,item)->{
            try {
                LightRpcService lightRpcService = (LightRpcService) item.getServiceInterface().getAnnotation(LightRpcService.class);
                String remote = "/light-rpc/" + lightRpcService.value() + "/providers";
                if(client.exists(remote)) {
                    List<String> urls = client.getChildren(remote);
                    Map<String,LightServiceNodeData> children = new HashMap<>();
                    urls.forEach(url->{
                        LightServiceNodeData data = client.readData(remote+"/"+url);
                        children.put(url,data);
                    });
                    item.freshUrl(children);
                }
                client.subscribeChildChanges(remote, ((parentPath, currentChilds) -> {
                    if(remote.equals(parentPath) && currentChilds!=null){
                        Map<String,LightServiceNodeData> children = new HashMap<>();
                        currentChilds.forEach(interfaceName->{
                            LightServiceNodeData data = client.readData(remote+"/"+interfaceName);
                            children.put(interfaceName,data);
                        });
                        item.freshUrl(children);
                    }
                }));
            }catch (Exception e){
                log.error(e.getMessage(),e);
            }
        });
        return client;
    }

    public final static class Scanner extends ClassPathBeanDefinitionScanner {
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
                try {
                    definition.getPropertyValues().add("serviceInterface", Class.forName(definition.getBeanClassName()));
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(),e);
                }
                definition.setBeanClass(LightProxyFactoryBean.class);
            }
            return beanDefinitions;
        }

        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            AnnotationMetadata metadata = beanDefinition.getMetadata();
            return metadata.isInterface()&& metadata.isIndependent();
        }
    }

}
