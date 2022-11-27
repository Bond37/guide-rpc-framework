package github.client;

import github.javaguide.spring.annotation.RpcScan;
import lombok.Setter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:25:00
 */
@RpcScan(basePackage = {"github.client"})
@SpringBootApplication
public class NettyClientMain {

    @Setter
    @Resource
    private HelloController helloController;

    public static void main(String[] args) throws IOException {
        SpringApplication.run(NettyClientMain.class);
    }
}
