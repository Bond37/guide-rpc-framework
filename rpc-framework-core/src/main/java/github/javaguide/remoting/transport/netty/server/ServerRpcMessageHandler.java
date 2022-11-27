package github.javaguide.remoting.transport.netty.server;

import github.javaguide.enums.CompressTypeEnum;
import github.javaguide.enums.RpcResponseCodeEnum;
import github.javaguide.enums.SerializationTypeEnum;
import github.javaguide.remoting.constants.RpcConstants;
import github.javaguide.remoting.dto.RpcMessage;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.dto.RpcResponse;
import github.javaguide.remoting.handler.RpcRequestHandler;
import io.netty.channel.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

/**
 * Customize the ChannelHandler of the server to process the data sent by the client.
 * <p>
 * 如果继承自 SimpleChannelInboundHandler 的话就不要考虑 ByteBuf 的释放 ，{@link SimpleChannelInboundHandler} 内部的
 * channelRead 方法会替你释放 ByteBuf ，避免可能导致的内存泄露问题。详见《Netty进阶之路 跟着案例学 Netty》
 *
 * @author shuang.kou
 * @createTime 2020年05月25日 20:44:00
 */
@Slf4j
@ChannelHandler.Sharable
public class ServerRpcMessageHandler extends SimpleChannelInboundHandler<RpcMessage> {

    private final RpcRequestHandler rpcRequestHandler;

    public ServerRpcMessageHandler() {
        this.rpcRequestHandler = new RpcRequestHandler();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, RpcMessage msg) {
        log.info("server receive msg: [{}] ", msg);
        if (msg.getMessageType() == RpcConstants.REQUEST_TYPE) {
            RpcMessage rpcMessage = RpcMessage.builder()
                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.RESPONSE_TYPE).build();
            // 普通RPC请求
            RpcRequest rpcRequest = (RpcRequest) msg.getData();
            Object result = null;
            try {
                // Execute the target method (the method the client needs to execute) and return the method result
                result = rpcRequestHandler.handle(rpcRequest);
            } catch (Exception e) {
                log.warn("rpc service handler occur error: ", e);
            }
            log.info(String.format("server get result: %s", Optional.ofNullable(result).map(Object::toString).orElse("null")));
            if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                rpcMessage.setData(rpcResponse);
            } else {
                RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                rpcMessage.setData(rpcResponse);
                log.error("not writable now, message dropped");
            }
            ctx.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        }
    }
}
