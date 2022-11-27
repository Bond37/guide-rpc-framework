import github.client.HelloController;
import github.client.NettyClientMain;
import github.javaguide.Hello;
import github.javaguide.HelloService;
import github.javaguide.spring.annotation.RpcReference;
import lombok.Setter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import javax.annotation.Resource;

/**
 * @author bond
 * @description
 * @date 2022/11/25
 */
@SpringBootTest(classes = NettyClientMain.class)
public class RpcClientTest {

    @Setter
    @Resource
    private HelloController helloController;

    @Setter
    @RpcReference(group = "test", version = "v2")
    private HelloService helloService;

    @Test
    public void testRpcReferenceProxy() {
        System.out.println("testRpcReferenceProxy");
    }

    @Test
    public void rpcTest() {
        helloController.test();
        helloService.hello(new Hello("abc", "test"));
    }

}
