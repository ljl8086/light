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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.*;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;
import java.util.UUID;

/**
 * 服务提供方自动扫描组件。
 * Created by sunny on 2017/8/2 0002.
 */
public class LightProvideAutoConfiguration implements ApplicationContextAware,BeanFactoryPostProcessor {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private ApplicationContext applicationContext;

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
    private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        Scanner scanner = new Scanner((BeanDefinitionRegistry) beanFactory);
        String scan = applicationContext.getEnvironment().getProperty("spring.light.scan");
        if(scan!=null && scan.trim().length()>0) {
            scanner.scan(scan.split(","));
        }

        //注册zkclient
        String zookeeperUrl = applicationContext.getEnvironment().getProperty("spring.light.zookeeper.url");
        //单位：毫秒
        Integer zookeeperTimeout = applicationContext.getEnvironment().getProperty("spring.light.zookeeper.timeout",Integer.class,30*1000);
        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ZkClient.class);
        beanDefinitionBuilder.addConstructorArgValue(zookeeperUrl);
        beanDefinitionBuilder.addConstructorArgValue(zookeeperTimeout);
        ((BeanDefinitionRegistry) beanFactory).registerBeanDefinition("zkClient",beanDefinitionBuilder.getBeanDefinition());

    }

    public final class Scanner extends ClassPathBeanDefinitionScanner {
        public Scanner(BeanDefinitionRegistry registry) {
            super(registry);
        }

        public void registerDefaultFilters() {
            this.addIncludeFilter(new AnnotationTypeFilter(LightRpcService.class));
        }

        /**
         * 将扫描到的服务实现类动态注册到spring上下文中。<br>
         * 为了更好的控制服务，这里注册的为动态代理对象。
         * @param basePackages
         * @return
         */
        public Set<BeanDefinitionHolder> doScan(String... basePackages) {
            Set<BeanDefinitionHolder> beanDefinitions =   super.doScan(basePackages);
            for (BeanDefinitionHolder holder : beanDefinitions) {
                try {
                    GenericBeanDefinition definition = (GenericBeanDefinition) holder.getBeanDefinition();
                    String token = UUID.randomUUID().toString().replace("-","");
                    Class remoteCls = remoteCls = Class.forName(definition.getBeanClassName());;

                    definition.setAutowireCandidate(true);
                    definition.setAutowireMode(GenericBeanDefinition.AUTOWIRE_BY_TYPE);

                    LightRpcService rpcService = (LightRpcService)remoteCls.getAnnotation(LightRpcService.class);
                    String remoteName = rpcService.value().trim()=="" ? remoteCls.getName() : rpcService.value();
                    String group = rpcService.group().trim()=="" ? "" : "/"+rpcService.group();

                    Object remoteObj = new RuntimeBeanReference(holder.getBeanName());
                    String name = group+"/"+remoteName;

                    ConfigurableApplicationContext context = (ConfigurableApplicationContext)applicationContext;
                    BeanDefinitionRegistry registry = (BeanDefinitionRegistry)context.getBeanFactory();
                    AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(LightHessianExporter.class);
                    abd.setPropertyValues(
                            new MutablePropertyValues()
                            .add("service", remoteObj)
                            .add("token",token)
                            .add("originalServiceCls",remoteCls)
                            .add("serviceInterface", remoteCls.getInterfaces()[0])
                    );
                    // 可以自动生成name
                    String beanName = (name != null ? name : LightProvideAutoConfiguration.this.beanNameGenerator.generateBeanName(abd, registry));
                    AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
                    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
                    BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, registry);
                    log.info("RPC接口spring上下文注册成功[{}]",remoteName);
                } catch (ClassNotFoundException e) {
                    log.error(e.getMessage(),e);
                }
            }
            return beanDefinitions;
        }

        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            AnnotationMetadata metadata = beanDefinition.getMetadata();
            return  metadata.isIndependent();
        }
    }
}
