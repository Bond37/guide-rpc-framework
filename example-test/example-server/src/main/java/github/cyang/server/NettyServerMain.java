package github.cyang.server;

import github.javaguide.spring.annotation.RpcScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Server: Automatic registration service via @RpcService annotation
 *
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
@SpringBootApplication
@EnableAspectJAutoProxy(proxyTargetClass = true)
@RpcScan(basePackage = {"github.cyang.server.service"})
public class NettyServerMain {
    public static void main(String[] args) {
        SpringApplication.run(NettyServerMain.class);
    }
}
