package github.javaguide.loadbalance.loadbalancer;

import github.javaguide.DemoRpcService;
import github.javaguide.DemoRpcServiceImpl;
import github.javaguide.config.ServiceDetail;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.loadbalance.LoadBalance;
import github.javaguide.remoting.dto.RpcRequest;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;


class ConsistentHashLoadBalanceTest {
    @Test
    void TestConsistentHashLoadBalance() {
        LoadBalance loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
        List<String> serviceUrlList = new ArrayList<>(Arrays.asList("127.0.0.1:9997", "127.0.0.1:9998", "127.0.0.1:9999"));

        DemoRpcService demoRpcService = new DemoRpcServiceImpl();
        ServiceDetail serviceDetail = ServiceDetail.builder()
                .group("test2").version("version2").serviceName(demoRpcService.getClass().getInterfaces()[0].getSimpleName()).build();

        RpcRequest rpcRequest = RpcRequest.builder()
                .parameters(demoRpcService.getClass().getTypeParameters())
                .interfaceName(serviceDetail.getServiceName())
                .requestId(UUID.randomUUID().toString())
                .group(serviceDetail.getGroup())
                .version(serviceDetail.getVersion())
                .build();
        String userServiceAddress = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        assertEquals("127.0.0.1:9998", userServiceAddress);
    }
}