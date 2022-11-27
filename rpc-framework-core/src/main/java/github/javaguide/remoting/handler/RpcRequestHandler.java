package github.javaguide.remoting.handler;

import github.javaguide.exception.RpcException;
import github.javaguide.provider.ServiceProvider;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.spring.util.ApplicationContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.Opt;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

/**
 * RpcRequest processor
 *
 * @author shuang.kou
 * @createTime 2020年05月13日 09:05:00
 */
@Slf4j
public class RpcRequestHandler {
    private ServiceProvider serviceProvider;

    /**
     * Processing rpcRequest: call the corresponding method, and then return the method
     */
    public Object handle(RpcRequest rpcRequest) {
        serviceProvider = Optional.ofNullable(serviceProvider).orElseGet(() -> ApplicationContextUtil.getBean(ServiceProvider.class));
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }

    /**
     * get method execution results
     *
     * @param rpcRequest client request
     * @param service    service object
     * @return the result of the target method execution
     */
    private Object invokeTargetMethod(RpcRequest rpcRequest, Object service) {
        Object result;
        try {
            Method method = service.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParamTypes());
            result = method.invoke(service, rpcRequest.getParameters());
            log.info("service:[{}] successful invoke method:[{}]", rpcRequest.getInterfaceName(), rpcRequest.getMethodName());
        } catch (NoSuchMethodException | IllegalArgumentException | InvocationTargetException | IllegalAccessException e) {
            throw new RpcException(e.getMessage(), e);
        }
        return result;
    }
}
