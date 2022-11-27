# guide-rpc-framework

## 介绍

 [guide-rpc-framework](https://github.com/Snailclimb/guide-rpc-framework) 是一款基于 Netty+Kyro+Zookeeper 实现的 RPC 框架，通过该项目学习Netty和RPC相关知识。



## 原项目已实现的功能点

- [x] **使用 Netty（基于 NIO）替代 BIO 实现网络传输；**
- [x] **使用开源的序列化机制 Kyro（也可以用其它的）替代 JDK 自带的序列化机制；**
- [x] **使用 Zookeeper 管理相关服务地址信息**
- [x] Netty 重用 Channel 避免重复连接服务端
- [x] 使用 `CompletableFuture` 包装接受客户端返回结果（之前的实现是通过 `AttributeMap` 绑定到 Channel 上实现的） 详见：[使用 CompletableFuture 优化接受服务提供端返回结果](./docs/使用CompletableFuture优化接受服务提供端返回结果.md)
- [x] **增加 Netty 心跳机制** : 保证客户端和服务端的连接不被断掉，避免重连。
- [x] **客户端调用远程服务的时候进行负载均衡** ：调用服务的时候，从很多服务地址中根据相应的负载均衡算法选取一个服务地址。ps：目前实现了随机负载均衡算法与一致性哈希算法。
- [x] **处理一个接口有多个类实现的情况** ：对服务分组，发布服务的时候增加一个 group 参数即可。
- [x] **集成 Spring 通过注解注册服务**
- [x] **集成 Spring 通过注解进行服务消费** 。参考： [PR#10](https://github.com/Snailclimb/guide-rpc-framework/pull/10)
- [x] **增加服务版本号** ：建议使用两位数字版本，如：1.0，通常在接口不兼容时版本号才需要升级。为什么要增加服务版本号？为后续不兼容升级提供可能，比如服务接口增加方法，或服务模型增加字段，可向后兼容，删除方法或删除字段，将不兼容，枚举类型新增字段也不兼容，需通过变更版本号升级。
- [x] **对 SPI 机制的运用** 
- [ ] **增加可配置比如序列化方式、注册中心的实现方式,避免硬编码** ：通过 API 配置，后续集成 Spring 的话建议使用配置文件的方式进行配置
- [x] **客户端与服务端通信协议（数据包结构）重新设计** ，可以将原有的 `RpcRequest`和 `RpcReuqest` 对象作为消息体，然后增加如下字段（可以参考：《Netty 入门实战小册》和 Dubbo 框架对这块的设计）：
  - **魔数** ： 通常是 4 个字节。这个魔数主要是为了筛选来到服务端的数据包，有了这个魔数之后，服务端首先取出前面四个字节进行比对，能够在第一时间识别出这个数据包并非是遵循自定义协议的，也就是无效数据包，为了安全考虑可以直接关闭连接以节省资源。
  - **序列化器编号** ：标识序列化的方式，比如是使用 Java 自带的序列化，还是 json，kyro 等序列化方式。
  - **消息体长度** ： 运行时计算出来。
  - ......
- [ ] **编写测试为重构代码提供信心**
- [ ] **服务监控中心（类似dubbo admin）**
- [x] **设置 gzip 压缩**



## 学习收获：

### 1. 注册中心

目前选了zookeeper，数据存储设计：`/{GROUP}/{VERSION}/{serviceName}/servers/ip:port`

先是对ServiceRegistry接口新增扩展了3个 `init、close、listServersForServiceName方法`

```java
void init();  // 负责初始化，建立连接
void close(); // 负责清理Cache和服务列表，关闭连接
// Provider注册服务接口
void registerService(List<String> serviceList){ zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path)
}
// Consumer根据获取服务接口获取Provider地址
List<String> listServersForServiceName(String rpcServiceName){
	List<String> result = zkClient.getChildren().forPath(rpcServiceName);
	// 缓存起来
	SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
    // 并添加pathChildrenCacheListener，监听serverAddress上下线时对SERVICE_ADDRESS_MAP维护
    new Thread(() -> registerWatcher(rpcServiceName, zkClient)).start();
}
```

- 废弃ServiceDiscovery SPI接口和ZkServiceDiscoveryImpl设计，ServiceDiscovery 注入ServiceRegistry，通过调用`listServersForServiceName`方法获取ProviderServerAddress即可
- 废弃原来的ServiceProvider接口ZkServiceProviderImpl设计，ServiceProvider只需要保留**serviceMap**用于存放`rpcServiceName->rpcSeviceObj` 键值对，另新增`fetchAllServiceName`方法用于返回所有**rpcServiceName**，用于ServiceRegistry注册服务，另外`registerService`操作延后spring容器刷新时候再发布，通过**监听器**实现（后文spring集成再介绍）

> 知识点
>
> CuratorFramework  **crud、cache** ，还可以拓展学习[discovery、leader、locking相关](https://github.com/apache/curator/tree/master/curator-examples/src/main/java)



### 2. netty网络编程：

#### 2.1 自定义协议

![image-20221127191408475](E:\project\guide-rpc-framework\images\custom-protocol.png)

由于TCP/IP 中消息传输基于流的方式，没有边界，协议的目的就是划定消息的边界，制定通信双方要共同遵守的通信规则。

* 魔数，用来在第一时间判定是否是无效数据包
* 版本号，可以支持协议的升级
* 消息长度
* 消息类型
* 序列化算法，消息正文到底采用哪种序列化反序列化方式，可以由此扩展，例如：json、protobuf、hessian、jdk
* 压缩类型，消息数据包是否压缩传输，比如使用gzip、snappy等
* 请求序号，为了双工通信，提供异步能力
* 正文长度
* 消息正文

#### 2.2 编解码器（粘包拆包）

处理粘包拆包问题，一般解决方案有

1. 短链接，发一个包建立一次连接，这样连接建立到连接断开之间就是消息的边界，缺点效率太低
2. 每一条消息采用固定长度，缺点浪费空间
3. 每一条消息采用分隔符，例如 \n，缺点需要转义
4. 每一条消息分为 head 和 body，head 中包含 body 的长度

这里使用LengthFieldBasedFrameDecoder ，在发送消息前，先约定用定长字节表示接下来消息的长度

> LengthFieldBasedFrameDecoder 参数
>
> maxFrameLength  最大长度
>
> lengthFieldOffset   消息长度字段偏移量
> lengthFieldLength   长度占用字节
> lengthAdjustment    长度调整 
> initialBytesToStrip  剥离字节数
>
> 默认读取的实际长度为：frameLength +=lengthFieldOffset+lengthFieldLength+lengthAdjustment

```java
@ChannelHandler.Sharable
public class RpcMessageCodec extends MessageToMessageCodec<ByteBuf, RpcMessage> {
	@Override
	void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list) {
        checkMagicNumber(in);
        checkVersion(in);
        int fullLength = in.readInt();
        // build RpcMessage object
        byte messageType = in.readByte();
        byte codecType = in.readByte();
        byte compressType = in.readByte();
        int requestId = in.readInt();
        ...
    }
	@Override
    void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, List<Object> list) {
        ByteBuf out = channelHandlerContext.alloc().buffer();
        out.writeBytes(RpcConstants.MAGIC_NUMBER);
        out.writeByte(RpcConstants.VERSION);
        // 修改写指针后移4个字节，留空到最后填写消息长度
        out.writerIndex(out.writerIndex() + 4);
        byte messageType = rpcMessage.getMessageType();
        out.writeByte(messageType);
        out.writeByte(rpcMessage.getCodec());
        out.writeByte(CompressTypeEnum.GZIP.getCode());
        // requestId自增
        out.writeInt(ATOMIC_INTEGER.getAndIncrement());
        // serialize the body object and compress the bytes
        // compute fullLength = head length + body length
        ...
        // 填写消息长度
        out.setInt(RpcConstants.MAGIC_NUMBER.length + 1, fullLength);
        list.add(out);
    }
}
```

#### 2.3 长连接和心跳机制

客户端：

```java
bootstrap.group(eventLoopGroup)
    .channel(NioSocketChannel.class)
    // TCP Keepalive 机制，实现 TCP 层级的心跳保活功能
    .option(ChannelOption.SO_KEEPALIVE, true) 
    // 允许较小的数据包的发送，降低延迟
    .option(ChannelOption.TCP_NODELAY, true)
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
```

服务端：

```java
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
```

```java
@ChannelHandler.Sharable
public class HeartBeatHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        RpcMessage rpcMessage = (RpcMessage) msg;
        byte messageType = rpcMessage.getMessageType();
        if (messageType == RpcConstants.HEARTBEAT_REQUEST_TYPE) {
            // 代表服务端收到客户端发送的心跳请求，则直接回复PONG
            ctx.channel().writeAndFlush(this.getHeartBeatResponseMessage())
                .addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
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
            // 服务端触发读空闲事件，客户端已下线，关闭连接
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }
    ...
}
```

#### 2.4 重用 Channel  && 异步接收响应

```java
    @Override
    public Promise sendRpcRequest(RpcRequest rpcRequest) {
        EventExecutor executor = serviceHandlerGroup.next();
        // 创建Promise用于异步接收结果
        Promise<RpcResponse<Object>> resultFuture = executor.newPromise();
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);
        // 客户端发消息时，通过channelManager获取server address关联的channel
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            // 将RequestId-> Promise(response) 存入unprocessedRequestsMap
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            ...
            // 发送rpc请求
            channel.writeAndFlush(rpcMessage)
                .addListener((ChannelFutureListener) future -> {
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
```

```java
@ChannelHandler.Sharable
public class ClientRpcMessageHandler extends SimpleChannelInboundHandler<RpcMessage> {
    ...
    /**
     * Read the message transmitted by the server
     */
    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, RpcMessage rpcMessage) {
        log.info("client receive msg: [{}]", rpcMessage);
        if (rpcMessage.getMessageType() == RpcConstants.RESPONSE_TYPE) {
            // rpcMessage带有requestId,可在客户端收到RpcResponse时将响应结果放入前面创建的Promise
            unprocessedRequests.complete((RpcResponse<Object>) rpcMessage.getData());
        }
    }
}
```

#### 2.6 发送超时和重试

```java
private Object retrySend(RpcRequest rpcRequest, int retry) {
    int timeout = serviceDetail.getTimeout();
    RpcResponse<Object> response = null;
    long beginTime = System.currentTimeMillis();
    // 调用超时和retry重试
    while (response == null && TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - beginTime) < timeout && retry > 0) {
        // 使用 Promise 包装接受客户端返回结果
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
```

#### 2.5 独立线程池负责业务处理

```
public class NettyRpcServer {
	private final EventLoopGroup bossGroup = new NioEventLoopGroup();
    private final EventLoopGroup workerGroup = new NioEventLoopGroup();
    private final DefaultEventExecutorGroup serviceHandlerGroup = 
    new DefaultEventExecutorGroup(Runtime.getRuntime().availableProcessors() * 2,
            new ThreadFactoryBuilder().setNameFormat("rpc-business-thread").setDaemon(false).build());
...          
	public void start() {
		serverBootstrap.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            ...
            // 独立的线程池处理Handler
            .addLast(serviceHandlerGroup, new ServerRpcMessageHandler());
    }
}
```

服务端收到`RpcConstants.REQUEST_TYPE`后，需要从ServiceProvider中取出对应的ServiceObj，然后反射调用对应方法进行业务处理，所以这里对于**ServerRpcMessageHandler** 使用独立的**EventLoopGroup** 处理，这样

**workerGroup**的线程可以一直处理IO读写。

关键代码 `io.netty.channel.AbstractChannelHandlerContext#invokeChannelRead()`

```java
static void invokeChannelRead(final AbstractChannelHandlerContext next, Object msg) {
    final Object m = next.pipeline.touch(ObjectUtil.checkNotNull(msg, "msg"), next);
    // 下一个 handler 的事件循环是否与当前的事件循环是同一个线程
    EventExecutor executor = next.executor();
    // 是，直接调用
    if (executor.inEventLoop()) {
        next.invokeChannelRead(m);
    } 
    // 不是，将要执行的代码作为任务提交给下一个事件循环处理
    else {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                next.invokeChannelRead(m);
            }
        });
    }
}
```

* 如果两个 handler 绑定的是同一个线程，那么就直接调用
* 否则，把要调用的代码封装为一个任务对象，由下一个 handler 的线程来调用

### 3. 序列化 & 压缩

序列化，反序列化主要用在消息体的转换上

```java
@ChannelHandler.Sharable
public class RpcMessageCodec extends MessageToMessageCodec<ByteBuf, RpcMessage> {
    void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> list){
        ...
        // 获取序列化类型
        String codecName = SerializationTypeEnum.getName(rpcMessage.getCodec());
log.info("codec name: [{}] ", codecName);
        // SPI获取序列化器
Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class)
        .getExtension(codecName);
        // 执行序列化
bodyBytes = serializer.serialize(rpcMessage.getData());
        // 获取压缩类型
        String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
        Compress compress = ExtensionLoader.getExtensionLoader(Compress.class)
            .getExtension(compressName);
        // 执行数据压缩
        bodyBytes = compress.compress(bodyBytes);
        ...
    }

    // 解压、反序列化同理
}
```

拓展了解常用的序列化方式，性能差异

### 4. rpc调用

#### 4.1 负载均衡

```java
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        List<String> serviceUrlList = serviceRegistry.listServersForServiceName(rpcServiceName);
        ...
        // ConsistentHashLoadBalance(一致性hash)和随机
        String targetServiceUrl = loadbalancer.selectServiceAddress(serviceUrlList, rpcRequest);
        ...
        return new InetSocketAddress(host, port);
    }
```

可拓展了解常见负载均衡策略：轮询、随机（加权）、一致性hash、最小连接..

#### 4.2 动态代理

```java
@RpcReference(group = "test", version = "v1")
HelloService helloService;
```

客户端使用@RpcReference标识RPC服务接口，当调用其接口方法时其实是执行一系列操作：

1. 根据HelloService接口+group+version组成RpcServiceName
2. serviceDiscovery获取RpcServiceName对应的ServiceProvider Address
3. nettyClient根据Address获取对应通信Channel
4. 构建RpcRequest，发送RpcRequestMessage
5. 等待获取ServiceProvider 的响应结果

```java
public class RpcClientProxy implements InvocationHandler {
    public <T> T getProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class<?>[]{clazz}, this);
    }
	
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (method.getName().equals("toString")) {
            return serviceDetail.getRpcServiceName();
        }
        log.info("invoked method: [{}]", method.getName());
        // 通过动态代理方式执行
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
}
```

#### 4.3 反射

```java
public class RpcRequestHandler {
// 服务端接收到RPC请求解析得到对应服务接口，通过反射执行对应方法
    public Object handle(RpcRequest rpcRequest) {
        serviceProvider = Optional.ofNullable(serviceProvider).orElseGet(() -> ApplicationContextUtil.getBean(ServiceProvider.class));
        Object service = serviceProvider.getService(rpcRequest.getRpcServiceName());
        return invokeTargetMethod(rpcRequest, service);
    }
}
```

### 5. spring集成

#### 5.1 @RpcScan 

在RpcScan 注解上@Import(AutoRpcServiceScannerRegistrar.class)

```java
public class AutoRpcServiceScannerRegistrar implements ImportBeanDefinitionRegistrar, ResourceLoaderAware, EnvironmentAware {
    private static final String SPRING_BEAN_BASE_PACKAGE = "github.javaguide";
    private static final String BASE_PACKAGE_ATTRIBUTE_NAME = "basePackage";
    private Environment environment;
    private ResourceLoader resourceLoader;
...

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata, BeanDefinitionRegistry registry) {
        String[] basePackage = getBasePackages(annotationMetadata);
        // process @RpcService
        ClassPathBeanDefinitionScanner rpcServiceScanner = new ClassPathBeanDefinitionScanner(registry, false,
                environment, resourceLoader);
        rpcServiceScanner.addIncludeFilter(new AnnotationTypeFilter(RpcService.class));
        rpcServiceScanner.scan(basePackage);
        ClassPathBeanDefinitionScanner rpcFrameworkScanner = new ClassPathBeanDefinitionScanner(registry, false,
                environment, resourceLoader);
        rpcFrameworkScanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        rpcFrameworkScanner.scan(SPRING_BEAN_BASE_PACKAGE);
    }
}
```

AutoRpcServiceScannerRegistrar 实现ImportBeanDefinitionRegistrar接口，通过ClassPathBeanDefinitionScanner扫描指定package实现注册BeanDifinition

#### 5.2 RpcAnnotationProcessor

```java
@Override
public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    // 对客户端上RpcReference注解的属性，通过动态代理封装成代理对象返回
    Class<?> targetClass = AopProxyUtils.ultimateTargetClass(bean);
    processRpcReferenceAnnotation(bean, targetClass);
    // 对服务端上RpcService注解，调用serviceProvider发布到zk
    processRpcServiceAnnotation(bean, targetClass);
    return bean;
}

private void processRpcServiceAnnotation(Object bean, Class<?> targetClass) {
    if (targetClass.isAnnotationPresent(RpcService.class)) {
        log.info("[{}] is annotated with  [{}]", targetClass.getName(), RpcService.class.getCanonicalName());
        // get RpcService annotation
        RpcService rpcService = targetClass.getAnnotation(RpcService.class);
        // build serviceDetail
        ServiceDetail serviceDetail = ServiceDetail.builder()
                .group(rpcService.group())
                .version(rpcService.version())
                .serviceName(targetClass.getInterfaces()[0].getSimpleName()).
                .serviceObj(bean).build();
        // 调整到AfterInitialization保存到serviceMap，解决：BeforeInitialization保存的是原始对象，导致AOP失效
        serviceProvider = Optional.ofNullable(serviceProvider).orElseGet(() -> beanFactory.getBean(ServiceProvider.class));
        serviceProvider.addService(serviceDetail);
    }
}

private void processRpcReferenceAnnotation(Object bean, Class<?> targetClass) {
    Field[] declaredFields = targetClass.getDeclaredFields();
    for (Field declaredField : declaredFields) {
        RpcReference rpcReference = declaredField.getAnnotation(RpcReference.class);
        if (rpcReference != null) {
            Class<?> type = declaredField.getType();
            ServiceDetail serviceDetail = ServiceDetail.builder().group(rpcReference.group())
                    .version(rpcReference.version()).serviceName(type.getSimpleName()).build();
            registryRpcReferenceClass(rpcReference, type, serviceDetail);
            // 通过FactoryBean.getObject 获取代理对象，RpcReference相同返回同一代理对象
            Object clientProxy = beanFactory.getBean(serviceDetail.getRpcServiceName());
            declaredField.setAccessible(true);
            try {
                declaredField.set(bean, clientProxy);
            } catch (IllegalAccessException e) {
                log.error("RpcReference Inject occur error", e);
            }
        }
    }
}

private void registryRpcReferenceClass(RpcReference rpcReference, Class<?> type, ServiceDetail serviceDetail) {
    if (beanFactory.containsBean(serviceDetail.getRpcServiceName())) {
        return;
    }
    ...
    AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(RpcServiceFactoryBean.class)
            .addConstructorArgValue(type)
            .addConstructorArgValue(nettyRpcClient)
            .addConstructorArgValue(serviceDetail).getBeanDefinition();
    BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
    // 注册成为FactoryBean
    registry.registerBeanDefinition(serviceDetail.getRpcServiceName(), beanDefinition);
}
```

RpcAnnotationProcessor 实现BeanPostProcessor接口，在postProcessAfterInitialization拦截，对bean上

**@RpcReference和@RpcService** 注解处理

RpcAnnotationProcessor 实现Orderd接口，设置低优先级，在AbstractAutoProxyCreator之后处理，**@RpcService**就能设置AOP代理对象

对使用**@RpcReference** 的成员属性，注册RpcServiceFactoryBean beanDefinition，通过FactoryBean.getObject 获取代理对象，设置到Field上。

#### 5.3 AutoRpcServiceConfiguration

springboot-starter 自动配置

```java
@Configuration
@EnableConfigurationProperties(RpcConfig.class)
public class AutoRpcServiceConfiguration {

    @ConditionalOnProperty(prefix = "rpc.registry", name = "type", havingValue = "zookeeper", matchIfMissing = true)
    @ConditionalOnClass(value = {org.apache.zookeeper.ZooKeeper.class})
    @Bean(initMethod = "init", destroyMethod = "close")
    public ZookeeperServiceRegistry zookeeperRegistryCenter(ZookeeperProperties zookeeperProperties, RpcConfig rpcConfig) {
        return new ZookeeperServiceRegistry(zookeeperProperties, rpcConfig);
    }

    @ConditionalOnProperty(value = "rpc.protocol.consumer", havingValue = "true")
    @Bean
    public NettyRpcClient nettyRpcClient(ServiceDiscovery serviceDiscovery) {
        return new NettyRpcClient(serviceDiscovery);
    }

    @ConditionalOnProperty(value = "rpc.protocol.provider", havingValue = "true")
    @Bean
    public NettyRpcServer nettyRpcServer(RpcConfig rpcConfig) {
        return new NettyRpcServer(rpcConfig);
    }

    @ConditionalOnProperty(value = "rpc.protocol.provider", havingValue = "true")
    @EventListener
    public void publishRpcServiceListener(ContextRefreshedEvent event) {
        // 服务暴露延迟到ContextRefreshed 时候，再调用serviceRegistry注册服务
        ApplicationContext applicationContext = event.getApplicationContext();
        ServiceProvider provider = applicationContext.getBean(ServiceProvider.class);
        ServiceRegistry serviceRegistry = applicationContext.getBean(ServiceRegistry.class);
        serviceRegistry.registerService(provider.fetchAllServiceName());
    }
}
```

spring.factories

```txt
org.springframework.boot.autoconfigure.EnableAutoConfiguration=github.javaguide.spring.AutoRpcServiceConfiguration
```
