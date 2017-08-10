package com.na.light;

import com.na.light.hessian.LightHessianExporter;
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
import org.springframework.beans.factory.support.*;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
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
@Configuration
@EnableConfigurationProperties()
public class LightRpcServiceAutoConfiguration implements ApplicationContextAware,BeanFactoryPostProcessor {
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private ApplicationContext applicationContext;

    private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();
    private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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

                log.info("RPC接口注册成功[{}]",remoteName);
            }
            return beanDefinitions;
        }

        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            AnnotationMetadata metadata = beanDefinition.getMetadata();
            return  metadata.isIndependent();
        }
    }
}
