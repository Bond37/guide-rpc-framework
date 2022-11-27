package github.javaguide;

import github.javaguide.spring.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;

/**
 * @author shuang.kou
 * @createTime 2020年05月10日 07:52:00
 */
@Slf4j
@RpcService(group = "dev", version = "v1")
public class DemoRpcServiceImpl2 implements DemoRpcService {

    @Override
    public String hello() {
        return "hello";
    }
}
