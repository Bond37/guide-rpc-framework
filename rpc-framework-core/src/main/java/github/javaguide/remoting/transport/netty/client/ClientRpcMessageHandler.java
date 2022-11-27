package github.javaguide.remoting.transport.netty.client;

import github.javaguide.remoting.constants.RpcConstants;
import github.javaguide.remoting.dto.RpcMessage;
import github.javaguide.remoting.dto.RpcResponse;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

/**
 * Customize the client ChannelHandler to process the data sent by the server
 *
 * <p>
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，{@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 *
 * @author shuang.kou
 * @createTime 2020年05月25日 20:50:00
 */
@Slf4j
@ChannelHandler.Sharable
public class ClientRpcMessageHandler extends SimpleChannelInboundHandler<RpcMessage> {
    private final UnprocessedRequests unprocessedRequests;

    public ClientRpcMessageHandler(UnprocessedRequests unprocessedRequests) {
        this.unprocessedRequests = unprocessedRequests;
    }

    /**
     * Read the message transmitted by the server
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage) {
        log.info("client receive msg: [{}]", rpcMessage);
        if (rpcMessage.getMessageType() == RpcConstants.RESPONSE_TYPE) {
            unprocessedRequests.complete((RpcResponse<Object>) rpcMessage.getData());
        }
    }
}

