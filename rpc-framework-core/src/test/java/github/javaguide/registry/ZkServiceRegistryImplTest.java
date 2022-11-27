package github.javaguide.registry;

import github.javaguide.DemoRpcService;
import github.javaguide.DemoRpcServiceImpl;
import github.javaguide.config.RpcConfig;
import github.javaguide.config.ServiceDetail;
import github.javaguide.registry.zk.ZookeeperProperties;
import github.javaguide.registry.zk.ZookeeperServiceRegistry;
import github.javaguide.remoting.dto.RpcRequest;
import org.apache.curator.test.TestingServer;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author shuang.kou
 * @createTime 2020年05月31日 16:25:00
 */
class ZkServiceRegistryImplTest {



    @Test
    void should_register_service_successful_and_lookup_service_by_service_name() throws Exception {
        TestingServer testingServer = new TestingServer(true);
        ZookeeperProperties zookeeperProperties = new ZookeeperProperties();
        zookeeperProperties.setServerLists(testingServer.getConnectString());
        RpcConfig rpcConfig = new RpcConfig();
        rpcConfig.setPort(9333);
        ServiceRegistry zkServiceRegistry = new ZookeeperServiceRegistry(zookeeperProperties, rpcConfig);
        zkServiceRegistry.init();
        InetSocketAddress givenInetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(),9333);
        DemoRpcService demoRpcService = new DemoRpcServiceImpl();
        ServiceDetail rpcServiceConfig = ServiceDetail.builder()
                .group("test2").version("version2").serviceName(demoRpcService.getClass().getInterfaces()[0].getSimpleName()).build();
        ArrayList<String> list = new ArrayList<>();
        list.add(rpcServiceConfig.getRpcServiceName());
        zkServiceRegistry.registerService(list);
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery();
        serviceDiscovery.setServiceRegistry(zkServiceRegistry);
        RpcRequest rpcRequest = RpcRequest.builder()
//                .parameters(args)
                .interfaceName(rpcServiceConfig.getServiceName())
//                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .group(rpcServiceConfig.getGroup())
                .version(rpcServiceConfig.getVersion())
                .build();
        InetSocketAddress acquiredInetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        assertEquals(givenInetSocketAddress.toString(), acquiredInetSocketAddress.toString());
    }
}
