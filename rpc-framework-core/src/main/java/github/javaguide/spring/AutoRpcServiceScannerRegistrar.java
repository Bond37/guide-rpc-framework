package github.javaguide.spring;

import github.javaguide.spring.annotation.RpcScan;
import github.javaguide.spring.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.StandardAnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

/**
 * scan and filter specified annotations
 *
 * @author shuang.kou
 * @createTime 2020年08月10日 22:12:00
 */
@Slf4j
public class AutoRpcServiceScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private static final String SPRING_BEAN_BASE_PACKAGE = "github.javaguide";
    private static final String BASE_PACKAGE_ATTRIBUTE_NAME = "basePackage";
    private Environment environment;
    private ResourceLoader resourceLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        String[] basePackage = getBasePackages(annotationMetadata);
        // process @RpcService
        ClassPathBeanDefinitionScanner rpcServiceScanner = new ClassPathBeanDefinitionScanner(registry, false,
                environment, resourceLoader);
        rpcServiceScanner.addIncludeFilter(new AnnotationTypeFilter(RpcService.class));
        rpcServiceScanner.scan(basePackage);
        ClassPathBeanDefinitionScanner rpcFrameworkScanner = new ClassPathBeanDefinitionScanner(registry, false,
                environment, resourceLoader);
        rpcFrameworkScanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        rpcFrameworkScanner.scan(SPRING_BEAN_BASE_PACKAGE);
    }

    private String[] getBasePackages(AnnotationMetadata annotationMetadata) {
        AnnotationAttributes rpcScanAnnotationAttributes = AnnotationAttributes.fromMap(annotationMetadata.getAnnotationAttributes(RpcScan.class.getName()));
        String[] basePackages = null;
        if (rpcScanAnnotationAttributes != null) {
            // get the value of the basePackage property
            basePackages = rpcScanAnnotationAttributes.getStringArray(BASE_PACKAGE_ATTRIBUTE_NAME);
        }
        if (basePackages == null) {
            String packageName = ((StandardAnnotationMetadata) annotationMetadata).getIntrospectedClass().getPackage().getName();
            basePackages = new String[]{packageName};
        }
        return basePackages;
    }

}
