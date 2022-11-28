package github.javaguide.remoting.transport.netty.server;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import github.javaguide.config.RpcConfig;
import github.javaguide.remoting.transport.netty.codec.ProtocolFrameDecoder;
import github.javaguide.remoting.transport.netty.codec.RpcMessageCodec;
import github.javaguide.remoting.transport.netty.handler.HeartBeatHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Server. Receive the client message, call the corresponding method according to the client message,
 * and then return the result to the client.
 *
 * @author shuang.kou
 * @createTime 2020年05月25日 16:42:00
 */
@Slf4j
public class NettyRpcServer {


    private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
            Runtime.getRuntime().availableProcessors() * 2,
            new ThreadFactoryBuilder().setNameFormat("rpc-business-thread").setDaemon(false).build());
    private static final Integer READ_TIMEOUT_SECONDS = 10;
    private Channel channel;
    private final int serverPort;

    public NettyRpcServer(RpcConfig rpcConfig) {
        this.serverPort = rpcConfig.getPort();
    }

    @PostConstruct
    public void start() {
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // TCP默认开启了 Nagle 算法，该算法的作用是尽可能的发送大数据快，减少网络传输。TCP_NODELAY 参数的作用就是控制是否启用 Nagle 算法。
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    // 是否开启 TCP 底层心跳机制
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // 表示系统用于临时存放已完成三次握手的请求的队列的最大长度,如果连接建立频繁，服务器处理创建新连接较慢，可以适当调大这个参数
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // 当客户端第一次进行请求的时候才会进行初始化
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast(new ProtocolFrameDecoder())
                                    .addLast(new RpcMessageCodec())
                                    // 5s 内如果没有收到 channel 的数据，会触发一个 IdleState#READER_IDLE 事件
                                    .addLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS))
                                    // 触发了读空闲事件则断开客户端连接
                                    .addLast(new HeartBeatHandler())
                                    // 独立的线程池处理Handler
                                    .addLast(serviceHandlerGroup, new ServerRpcMessageHandler());
                        }
                    });

            // 绑定端口，阻塞等到绑定成功
            channel = serverBootstrap.bind(serverPort).sync().channel();
        } catch (Exception e) {
            log.error("occur exception when start server:", e);
            close();
        }
    }

    @SneakyThrows
    @PreDestroy
    public void close() {
        channel.close();
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        serviceHandlerGroup.shutdownGracefully();
    }
}
