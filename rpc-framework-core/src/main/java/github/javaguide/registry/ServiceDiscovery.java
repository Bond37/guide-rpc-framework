package github.javaguide.registry;

import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.extension.ExtensionLoader;
import github.javaguide.loadbalance.LoadBalance;
import github.javaguide.remoting.dto.RpcRequest;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import javax.annotation.Resource;
import java.net.InetSocketAddress;
import java.util.List;

/**
 * service discovery based on zookeeper
 *
 * @author shuang.kou
 * @createTime 2020年06月01日 15:16:00
 */
@Slf4j
@Component
public class ServiceDiscovery {
    private final LoadBalance loadBalance;
    @Setter
    @Resource
    private ServiceRegistry serviceRegistry;

    public ServiceDiscovery() {
        this.loadBalance = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
    }

    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        List<String> serviceUrlList = serviceRegistry.listServersForServiceName(rpcServiceName);
        if (CollectionUtils.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND, rpcServiceName);
        }
        // load balancing
        String targetServiceUrl = loadBalance.selectServiceAddress(serviceUrlList, rpcRequest);
        log.info("Successfully found the service address:[{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }
}
