package github.javaguide.spring.annotation;

import github.javaguide.spring.AutoRpcServiceScannerRegistrar;
import org.springframework.context.annotation.Import;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * scan custom annotations
 *
 * @author shuang.kou
 * @createTime 2020年08月10日 21:42:00
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Import(AutoRpcServiceScannerRegistrar.class)
@Documented
public @interface RpcScan {

    String[] basePackage();

}
