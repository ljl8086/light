package com.na.light;

import com.na.light.hessian.LightProxyFactoryBean;
import org.I0Itec.zkclient.ZkClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.Set;

/**
 * Created by sunny on 2017/8/2 0002.
 */
public class LightRpcClientAutoConfiguration implements ApplicationContextAware,BeanFactoryPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(LightRpcClientAutoConfiguration.class);
    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
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
