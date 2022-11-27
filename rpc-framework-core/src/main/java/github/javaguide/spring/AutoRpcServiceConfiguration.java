package github.javaguide.spring;

import github.javaguide.config.RpcConfig;
import github.javaguide.provider.ServiceProvider;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.registry.ServiceRegistry;
import github.javaguide.registry.zk.ZookeeperProperties;
import github.javaguide.registry.zk.ZookeeperServiceRegistry;
import github.javaguide.remoting.transport.netty.client.NettyRpcClient;
import github.javaguide.remoting.transport.netty.server.NettyRpcServer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Configuration
@EnableConfigurationProperties(RpcConfig.class)
public class AutoRpcServiceConfiguration {

    /**
     * Create a zookeeper registry center bean via factory.
     *
     * @return zookeeper registry center
     */
    @ConditionalOnProperty(prefix = "rpc.registry", name = "type", havingValue = "zookeeper", matchIfMissing = true)
    @ConditionalOnClass(value = {org.apache.zookeeper.ZooKeeper.class})
    @Bean(initMethod = "init", destroyMethod = "close")
    public ZookeeperServiceRegistry zookeeperRegistryCenter(ZookeeperProperties zookeeperProperties, RpcConfig rpcConfig) {
        return new ZookeeperServiceRegistry(zookeeperProperties, rpcConfig);
    }


    @ConditionalOnProperty(value = "rpc.protocol.consumer", havingValue = "true")
    @Bean
    public NettyRpcClient nettyRpcClient(ServiceDiscovery serviceDiscovery) {
        return new NettyRpcClient(serviceDiscovery);
    }

    @ConditionalOnProperty(value = "rpc.protocol.provider", havingValue = "true")
    @Bean
    public NettyRpcServer nettyRpcServer(RpcConfig rpcConfig) {
        return new NettyRpcServer(rpcConfig);
    }

    @ConditionalOnProperty(value = "rpc.protocol.provider", havingValue = "true")
    @EventListener
    public void publishRpcServiceListener(ContextRefreshedEvent event) {
        ApplicationContext applicationContext = event.getApplicationContext();
        ServiceProvider provider = applicationContext.getBean(ServiceProvider.class);
        ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
        serviceRegistry.registerService(provider.fetchAllServiceName());
    }
}