package github.javaguide.remoting.transport;

import github.javaguide.extension.SPI;
import github.javaguide.remoting.dto.RpcRequest;
import io.netty.util.concurrent.Promise;

/**
 * send RpcRequest。
 *
 * @author shuang.kou
 * @createTime 2020年05月29日 13:26:00
 */
@SPI
public interface RpcRequestTransport {
    /**
     * send rpc request to server and get result
     *
     * @param rpcRequest message body
     * @return data from server
     */
    Promise sendRpcRequest(RpcRequest rpcRequest);
}
