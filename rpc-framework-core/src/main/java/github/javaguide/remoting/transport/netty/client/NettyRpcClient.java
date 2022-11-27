package github.javaguide.remoting.transport.netty.client;


import com.google.common.util.concurrent.ThreadFactoryBuilder;
import github.javaguide.enums.CompressTypeEnum;
import github.javaguide.enums.SerializationTypeEnum;
import github.javaguide.registry.ServiceDiscovery;
import github.javaguide.remoting.constants.RpcConstants;
import github.javaguide.remoting.dto.RpcMessage;
import github.javaguide.remoting.dto.RpcRequest;
import github.javaguide.remoting.dto.RpcResponse;
import github.javaguide.remoting.transport.RpcRequestTransport;
import github.javaguide.remoting.transport.netty.handler.HeartBeatHandler;
import github.javaguide.remoting.transport.netty.codec.ProtocolFrameDecoder;
import github.javaguide.remoting.transport.netty.codec.RpcMessageCodec;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.*;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.net.InetSocketAddress;

/**
 * initialize and close Bootstrap object
 *
 * @author shuang.kou
 * @createTime 2020年05月29日 17:51:00
 */
@Slf4j
public final class NettyRpcClient implements RpcRequestTransport {
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroup = new NioEventLoopGroup();
    private final DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
            Runtime.getRuntime().availableProcessors() * 2,
            new ThreadFactoryBuilder().setNameFormat("rpc-business-thread").setDaemon(false).build());

    private final ChannelManager channelManager = new ChannelManager();
    private final UnprocessedRequests unprocessedRequests = new UnprocessedRequests();
    private final ServiceDiscovery serviceDiscovery;
    private static final Integer WRITE_TIMEOUT_SECONDS = 5;

    public NettyRpcClient(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    @PostConstruct
    public void start() {
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true) // TCP Keepalive 机制，实现 TCP 层级的心跳保活功能
                .option(ChannelOption.TCP_NODELAY, true) // 允许较小的数据包的发送，降低延迟
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new LoggingHandler(LogLevel.INFO))
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception {
                        channel.pipeline()
                                .addLast(new ProtocolFrameDecoder())
                                .addLast(new RpcMessageCodec())
                                // 5s 内如果没有向服务器写数据，会触发一个 IdleState#WRITER_IDLE 事件
                                .addLast(new IdleStateHandler(0, WRITE_TIMEOUT_SECONDS, 0))
                                // 触发了写空闲事件则发送心跳
                                .addLast(new HeartBeatHandler())
                                .addLast(new ClientRpcMessageHandler(unprocessedRequests));
                    }
                });
    }

    @PreDestroy
    public void close() {
        eventLoopGroup.shutdownGracefully();
        serviceHandlerGroup.shutdownGracefully();
    }

    @Override
    public Promise sendRpcRequest(RpcRequest rpcRequest) {
        EventExecutor executor = serviceHandlerGroup.next();
        // build return value
        Promise<RpcResponse<Object>> resultFuture = executor.newPromise();
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // get  server address related channel
        Channel channel = getChannel(inetSocketAddress);

        if (channel.isActive()) {
            // put unprocessed request
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder().data(rpcRequest)
                    .codec(SerializationTypeEnum.HESSIAN.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE).build();
            // rpc请求
            channel.writeAndFlush(rpcMessage).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    log.info("client send message: [{}]", rpcMessage);
                } else {
                    future.channel().close();
                    resultFuture.setFailure(future.cause());
                    log.error("Send failed:", future.cause());
                }
            });
        } else {
            resultFuture.setFailure(new IllegalStateException("remote call fail: " + inetSocketAddress.toString()));
        }
        return resultFuture;
    }


    public Channel getChannel(InetSocketAddress inetSocketAddress) {
        Channel channel = channelManager.get(inetSocketAddress);
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelManager.set(inetSocketAddress, channel);
        }
        return channel;
    }

    /**
     * connect server and get the channel ,so that you can send rpc message to server
     *
     * @param inetSocketAddress server address
     * @return the channel
     */
    @SneakyThrows
    public Channel doConnect(InetSocketAddress inetSocketAddress) {
        Channel channel = bootstrap.connect(inetSocketAddress).sync().channel();
        // 连接关闭时需从channelProvider移除
        channel.closeFuture().addListener((ChannelFutureListener) future -> channelManager.remove(inetSocketAddress));
        return channel;
    }
}
