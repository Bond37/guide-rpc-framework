package github.cyang.server.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * @author bond
 * @description
 * @date 2022/11/27
 */
@Aspect
@Component
@Slf4j
public class RpcServiceAspect {
    @Pointcut("within(github.cyang.server.service.*)")
    public void aopPoint() {
    }

    @After("aopPoint()")
    public void after() {
        log.info("remote call after...");
    }

}
