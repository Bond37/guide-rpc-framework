package github.javaguide.registry;

import github.javaguide.extension.SPI;
import java.util.List;

/**
 * service registration
 *
 * @author shuang.kou
 * @createTime 2020年05月13日 08:39:00
 */
@SPI
public interface ServiceRegistry {


    /**
     * Initialize registry center.
     */
    void init();

    /**
     * Close registry center.
     */
    void close();

    /**
     * register service
     *
     * @param serviceList    rpc service name list
     */
    void registerService(List<String> serviceList);

    /**
     * list servers for rpcServiceName
     * @param rpcServiceName rpc serviceName
     * @return rpcService provider Address
     */
    List<String> listServersForServiceName(String rpcServiceName);

}
