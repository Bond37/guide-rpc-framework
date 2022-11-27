package github.javaguide.spring;

import github.javaguide.config.RpcConfig;
import github.javaguide.config.ServiceDetail;
import github.javaguide.provider.ServiceProvider;
import github.javaguide.remoting.transport.RpcRequestTransport;
import github.javaguide.spring.annotation.RpcReference;
import github.javaguide.spring.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.*;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import java.lang.reflect.Field;
import java.util.Optional;

/**
 * call this method before creating the bean to see if the class is annotated
 *
 * @author shuang.kou
 * @createTime 2020年07月14日 16:42:00
 */
@Slf4j
@Component
public class RpcAnnotationProcessor implements BeanPostProcessor, BeanFactoryAware, Ordered {
    private BeanFactory beanFactory;
    private RpcRequestTransport nettyRpcClient;
    private ServiceProvider serviceProvider;
    private RpcConfig rpcConfig;


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        // 对客户端上RpcReference注解的属性，通过动态代理封装成代理对象返回
        Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
        processRpcReferenceAnnotation(bean, targetClass);
        // 对服务端上RpcService注解，调用serviceProvider发布到zk
        processRpcServiceAnnotation(bean, targetClass);
        return bean;
    }

    private void processRpcServiceAnnotation(Object bean, Class<?> targetClass) {
        if (targetClass.isAnnotationPresent(RpcService.class)) {
            log.info("[{}] is annotated with  [{}]", targetClass.getName(), RpcService.class.getCanonicalName());
            // get RpcService annotation
            RpcService rpcService = targetClass.getAnnotation(RpcService.class);
            // build serviceDetail
            ServiceDetail serviceDetail = ServiceDetail.builder()
                    .group(rpcService.group())
                    .version(rpcService.version())
                    .serviceName(targetClass.getInterfaces()[0].getSimpleName())
                    .serviceObj(bean).build();
            // 延后到AfterInitialization保存到serviceMap，修复：BeforeInitialization保存的是原始对象，丢失了代理对象
            serviceProvider = Optional.ofNullable(serviceProvider).orElseGet(() -> beanFactory.getBean(ServiceProvider.class));
            serviceProvider.addService(serviceDetail);
        }
    }

    private void processRpcReferenceAnnotation(Object bean, Class<?> targetClass) {
        Field[] declaredFields = targetClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                Class<?> type = declaredField.getType();
                ServiceDetail serviceDetail = ServiceDetail.builder().group(rpcReference.group())
                        .version(rpcReference.version()).serviceName(type.getSimpleName()).build();
                registryRpcReferenceClass(rpcReference, type, serviceDetail);
                // 通过FactoryBean.getObject 获取代理对象，同时RpcReference相同返回同一代理对象
                Object clientProxy = beanFactory.getBean(serviceDetail.getRpcServiceName());
                declaredField.setAccessible(true);
                try {
                    declaredField.set(bean, clientProxy);
                } catch (IllegalAccessException e) {
                    log.error("RpcReference Inject occur error", e);
                }
            }
        }
    }

    private void registryRpcReferenceClass(RpcReference rpcReference, Class<?> type, ServiceDetail serviceDetail) {
        if (beanFactory.containsBean(serviceDetail.getRpcServiceName())) {
            return;
        }
        nettyRpcClient = Optional.ofNullable(nettyRpcClient).orElseGet(() -> beanFactory.getBean(RpcRequestTransport.class));
        rpcConfig = Optional.ofNullable(rpcConfig).orElseGet(() -> beanFactory.getBean(RpcConfig.class));
        serviceDetail.setRetries(rpcReference.retries() == 0 ? rpcConfig.getRetries() : rpcReference.retries());
        serviceDetail.setTimeout(rpcReference.timeout() == 0 ? rpcConfig.getTimeout() : rpcReference.timeout());
        AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(RpcServiceFactoryBean.class)
                .addConstructorArgValue(type)
                .addConstructorArgValue(nettyRpcClient)
                .addConstructorArgValue(serviceDetail).getBeanDefinition();
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        // 注册成为FactoryBean
        registry.registerBeanDefinition(serviceDetail.getRpcServiceName(), beanDefinition);
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        this.beanFactory = beanFactory;
    }
}
