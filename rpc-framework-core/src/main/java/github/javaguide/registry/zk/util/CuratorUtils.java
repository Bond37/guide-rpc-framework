package github.javaguide.registry.zk.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.*;
import org.apache.zookeeper.CreateMode;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_ADDED;
import static org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent.Type.CHILD_REMOVED;

/**
 * Curator(zookeeper client) utils
 *
 * @author shuang.kou
 * @createTime 2020年05月31日 11:38:00
 */
@Slf4j
public final class CuratorUtils {
    private static final Map<String, CuratorCache> CACHES = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();
    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();

    /**
     * Create persistent nodes. Unlike temporary nodes, persistent nodes are not removed when the client disconnects
     *
     * @param path node path
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            if (zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                //eg: namespace/{GROUP}/{VERSION}/{serviceName}/servers/127.0.0.1:9999
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
            REGISTERED_PATH_SET.add(path);
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * Empty the registry of data
     */
    public static void clearRegistry(CuratorFramework zkClient, String hostAndPort) {
        clearCache();
        REGISTERED_PATH_SET.stream().parallel().filter(p -> p.endsWith(hostAndPort)).forEach(p -> {
            try {
                zkClient.delete().forPath(p);
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });
        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET);
    }

    /**
     * Gets the children under a node
     *
     * @param rpcServiceName rpc service name eg:github.javaguide.HelloServicetest2version1
     * @return All child nodes under the specified node
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }
        List<String> result = null;
        try {
            result = zkClient.getChildren().forPath(rpcServiceName);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            new Thread(() -> registerWatcher(rpcServiceName, zkClient)).start();
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", rpcServiceName);
        }
        return result;
    }

    /**
     * Registers to listen for changes to the specified node
     *
     * @param servicePath rpc service name eg:/{GROUP}/{VERSION}/{serviceName}/servers
     */
    private static void registerWatcher(String servicePath, CuratorFramework zkClient) {
        CuratorCache pathChildrenCache = CuratorCache.build(zkClient, servicePath, CuratorCache.Options.SINGLE_NODE_CACHE);
        CuratorCacheListener pathChildrenCacheListener = CuratorCacheListener.builder()
                .forPathChildrenCache(servicePath, zkClient, (client, event) -> {
                    PathChildrenCacheEvent.Type eventType = event.getType();
                    if (eventType != CHILD_ADDED && eventType != CHILD_REMOVED) {
                        return;
                    }
                    List<String> serviceAddress = SERVICE_ADDRESS_MAP.get(servicePath);
                    String server = event.getData().getPath().substring(servicePath.length());
                    if (eventType == CHILD_ADDED) {
                        serviceAddress.add(server);
                        return;
                    }
                    serviceAddress.remove(server);
                }).build();
        pathChildrenCache.listenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
        CACHES.put(servicePath, pathChildrenCache);
    }

    public static void clearCache() {
        for (Map.Entry<String, CuratorCache> each : CACHES.entrySet()) {
            each.getValue().close();
        }
    }

}
