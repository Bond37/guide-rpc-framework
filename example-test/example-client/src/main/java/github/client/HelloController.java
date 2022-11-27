package github.client;

import github.javaguide.Hello;
import github.javaguide.HelloService;
import github.javaguide.spring.annotation.RpcReference;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

/**
 * @author smile2coder
 */
@Component
public class HelloController {

    @Setter
    @RpcReference(group = "test")
    private HelloService helloService;

    @Getter
    @Setter
    @RpcReference(group = "test", version = "v2")
    private HelloService helloService2;

    public void test() {
        String hello = this.helloService.hello(new Hello("111", "222"));
        System.out.println(hello);
    }
}
