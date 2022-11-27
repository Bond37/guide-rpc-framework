package github.javaguide.provider;

import github.javaguide.config.ServiceDetail;
import github.javaguide.enums.RpcErrorMessageEnum;
import github.javaguide.exception.RpcException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author shuang.kou
 * @createTime 2020年05月13日 11:23:00
 */
@Slf4j
@Component
public class ServiceProvider {
    /**
     * key: rpc service name(interface name + version + group)
     * value: service object
     */
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();

    public Object getService(String rpcServiceName) {
        Object service = serviceMap.get(rpcServiceName);
        if (null == service) {
            throw new RpcException(RpcErrorMessageEnum.SERVICE_CAN_NOT_BE_FOUND);
        }
        return service;
    }

    public void addService(ServiceDetail serviceDetail) {
        serviceMap.put(serviceDetail.getRpcServiceName(), serviceDetail.getServiceObj());
    }

    public List<String> fetchAllServiceName() {
        return new ArrayList<>(serviceMap.keySet());
    }

}
