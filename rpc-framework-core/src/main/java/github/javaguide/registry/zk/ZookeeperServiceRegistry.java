package github.javaguide.registry.zk;

import github.javaguide.config.RpcConfig;
import github.javaguide.registry.ServiceRegistry;
import github.javaguide.registry.zk.util.CuratorUtils;
import github.javaguide.registry.zk.util.RpcServicePathUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.KeeperException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * service registration  based on zookeeper
 *
 * @author shuang.kou
 * @createTime 2020年05月31日 10:56:00
 */
@Slf4j
public class ZookeeperServiceRegistry implements ServiceRegistry {
    private static CuratorFramework zkClient;
    private final ZookeeperProperties zkConfig;
    private String cachedIpAddress;
    private final int rpcServerPort;

    public ZookeeperServiceRegistry(ZookeeperProperties zkConfig, RpcConfig rpcConfig) {
        this.zkConfig = zkConfig;
        this.rpcServerPort = rpcConfig.getPort();
    }

    @Override
    public void init() {
        log.debug("zookeeper registry center init, server lists is: {}.", zkConfig.getServerLists());
        CuratorFrameworkFactory.Builder builder = CuratorFrameworkFactory.builder()
                .connectString(zkConfig.getServerLists())
                .retryPolicy(new ExponentialBackoffRetry(zkConfig.getRetryPolicy().getBaseSleepTime(),
                        zkConfig.getRetryPolicy().getMaxRetries(), zkConfig.getRetryPolicy().getMaxSleep()))
                .namespace(zkConfig.getNamespace());
        if (0 != zkConfig.getSessionTimeout()) {
            builder.sessionTimeoutMs(zkConfig.getSessionTimeout());
        }
        if (0 != zkConfig.getConnectionTimeout()) {
            builder.connectionTimeoutMs(zkConfig.getConnectionTimeout());
        }
        zkClient = builder.build();
        zkClient.start();
        try {
            if (!zkClient.blockUntilConnected(zkConfig.getBlockUntilConnected() * zkConfig.getRetryPolicy().getMaxRetries(), TimeUnit.SECONDS)) {
                zkClient.close();
                throw new KeeperException.OperationTimeoutException();
            }
        } catch (final Exception ex) {
            log.error("Registry center exception ", ex);
        }
    }

    @Override
    public void close() {
        CuratorUtils.clearRegistry(zkClient, cachedIpAddress);
        waitForCacheClose();
        zkClient.close();
    }

    /*
     * sleep 500ms, let cache client close first and then client, otherwise will throw exception
     * reference：https://issues.apache.org/jira/browse/CURATOR-157
     */
    private void waitForCacheClose() {
        try {
            Thread.sleep(500L);
        } catch (final InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void registerService(List<String> serviceList) {
        try {
            String host = InetAddress.getLocalHost().getHostAddress();
            cachedIpAddress = host + ":" + rpcServerPort;
        } catch (UnknownHostException e) {
            log.error("occur exception when getHostAddress", e);
        }
        serviceList.forEach(rpcServiceName -> CuratorUtils.createPersistentNode(zkClient,
                RpcServicePathUtil.getFullPath(rpcServiceName, cachedIpAddress)));
    }

    @Override
    public List<String> listServersForServiceName(String rpcServiceName) {
        return CuratorUtils.getChildrenNodes(zkClient, RpcServicePathUtil.getServerNodePath(rpcServiceName));
    }
}
