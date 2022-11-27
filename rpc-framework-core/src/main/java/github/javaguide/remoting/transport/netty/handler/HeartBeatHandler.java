package github.javaguide.remoting.transport.netty.handler;

import github.javaguide.enums.CompressTypeEnum;
import github.javaguide.enums.SerializationTypeEnum;
import github.javaguide.remoting.constants.RpcConstants;
import github.javaguide.remoting.dto.RpcMessage;
import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ChannelHandler.Sharable
public class HeartBeatHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RpcMessage rpcMessage = (RpcMessage) msg;
        byte messageType = rpcMessage.getMessageType();
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            // 代表服务端收到客户端发送的心跳请求，则直接回复PONG
            ctx.channel().writeAndFlush(this.getHeartBeatResponseMessage()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
        } else if (messageType == RpcConstants.HEARTBEAT_RESPONSE_TYPE) {
            // 代表客户端收到服务端心跳请求回复
            log.info("received heartbeat [{}]", rpcMessage.getData());
        } else {
            // 如果不是心跳消息类型，则交给下一个InboundHandle处理
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            // 客户端触发写空闲事件，发送心跳
            if (state == IdleState.WRITER_IDLE) {
                log.info("write idle happen [{}]", ctx.channel().remoteAddress());
                ctx.writeAndFlush(this.getHeartBeatRequestMessage()).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
            // 服务端触发读空闲事件，已客户端下线，关闭连接
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    private RpcMessage getHeartBeatResponseMessage() {
        return RpcMessage.builder()
                .codec(SerializationTypeEnum.HESSIAN.getCode())
                .compress(CompressTypeEnum.GZIP.getCode())
                .messageType(RpcConstants.HEARTBEAT_RESPONSE_TYPE)
                .data(RpcConstants.PONG)
                .build();
    }

    private RpcMessage getHeartBeatRequestMessage() {
        return RpcMessage.builder()
                .codec(SerializationTypeEnum.PROTOSTUFF.getCode())
                .compress(CompressTypeEnum.GZIP.getCode())
                .messageType(RpcConstants.HEARTBEAT_REQUEST_TYPE)
                .data(RpcConstants.PING)
                .build();
    }

    /**
     * Called when an exception occurs in processing a client message
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }

}

