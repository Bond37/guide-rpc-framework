package github.javaguide.remoting.transport.netty.client;

import github.javaguide.enums.RpcResponseCodeEnum;
import github.javaguide.exception.RpcException;
import github.javaguide.remoting.dto.RpcResponse;
import io.netty.util.concurrent.Promise;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * unprocessed requests by the server.
 *
 * @author shuang.kou
 * @createTime 2020年06月04日 17:30:00
 */
public class UnprocessedRequests {
    private static final Map<String, Promise<RpcResponse<Object>>> UNPROCESSED_RESPONSE_FUTURES = new ConcurrentHashMap<>();

    public void put(String requestId, Promise<RpcResponse<Object>> future) {
        UNPROCESSED_RESPONSE_FUTURES.put(requestId, future);
    }

    public void complete(RpcResponse<Object> rpcResponse) {
        Promise<RpcResponse<Object>> future = UNPROCESSED_RESPONSE_FUTURES.remove(rpcResponse.getRequestId());
        if (null != future) {
            if (rpcResponse.getCode() != null || rpcResponse.getCode().equals(RpcResponseCodeEnum.SUCCESS.getCode())) {
                future.setSuccess(rpcResponse);
            } else {
                future.setFailure(new RpcException(rpcResponse.getMessage()));
            }
        }
    }
}
