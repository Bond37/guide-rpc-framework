package github.javaguide.proxy;

import github.javaguide.config.ServiceDetail;
import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.dto.RpcResponse;
import github.javaguide.remoting.transport.RpcRequestTransport;
import io.netty.util.concurrent.Promise;
import lombok.extern.slf4j.Slf4j;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Dynamic proxy class.
 * When a dynamic proxy object calls a method, it actually calls the following invoke method.
 * It is precisely because of the dynamic proxy that the remote method called by the client is like calling the local method (the intermediate process is shielded)
 *
 * @author shuang.kou
 * @createTime 2020年05月10日 19:01:00
 */
@Slf4j
public class RpcClientProxy implements InvocationHandler {

    private static final String INTERFACE_NAME = "interfaceName";
    private final RpcRequestTransport rpcRequestTransport;
    private final ServiceDetail serviceDetail;


    public RpcClientProxy(RpcRequestTransport rpcRequestTransport, ServiceDetail serviceDetail) {
        this.rpcRequestTransport = rpcRequestTransport;
        this.serviceDetail = serviceDetail;
    }

    /**
     * get the proxy object
     */
    @SuppressWarnings("unchecked")
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }

    /**
     * This method is actually called when you use a proxy object to call a method.
     * The proxy object is the object you get through the getProxy method.
     */
    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("toString")) {
            return serviceDetail.getRpcServiceName();
        }
        log.info("invoked method: [{}]", method.getName());
        RpcRequest rpcRequest = RpcRequest.builder().methodName(method.getName())
                .parameters(args)
                .interfaceName(serviceDetail.getServiceName())
                .paramTypes(method.getParameterTypes())
                .requestId(UUID.randomUUID().toString())
                .group(serviceDetail.getGroup())
                .version(serviceDetail.getVersion())
                .build();
        return retrySend(rpcRequest, serviceDetail.getRetries());
    }

    private Object retrySend(RpcRequest rpcRequest, int retry) {
        int timeout = serviceDetail.getTimeout();
        RpcResponse<Object> response = null;
        long beginTime = System.currentTimeMillis();
        while (response == null && TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - beginTime) < timeout && retry > 0) {
            // 使用 Promise 包装接受客户端返回结果,超时报错
            Promise<RpcResponse<Object>> resultFuture = (Promise<RpcResponse<Object>>) rpcRequestTransport.sendRpcRequest(rpcRequest);
            try {
                response = resultFuture.get(timeout, TimeUnit.SECONDS);
            } catch (Exception e) {
                retry--;
            }
        }
        if (response == null) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_INVOCATION_FAILURE, INTERFACE_NAME + ":" + rpcRequest.getInterfaceName());
        }
        return response.getData();
    }
}
