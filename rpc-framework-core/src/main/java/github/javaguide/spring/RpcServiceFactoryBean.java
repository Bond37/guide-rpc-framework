package github.javaguide.spring;

import github.javaguide.config.ServiceDetail;
import github.javaguide.proxy.RpcClientProxy;
import github.javaguide.remoting.transport.RpcRequestTransport;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.FactoryBean;

@Slf4j
@AllArgsConstructor
public class RpcServiceFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> serviceInterface;
    private final RpcRequestTransport rpcClient;
    private final ServiceDetail serviceDetail;

    @Override
    public T getObject() {
        // 返回代理对象
        RpcClientProxy rpcClientProxy = new RpcClientProxy(rpcClient, serviceDetail);
        return rpcClientProxy.getProxy(serviceInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return serviceInterface;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

}
