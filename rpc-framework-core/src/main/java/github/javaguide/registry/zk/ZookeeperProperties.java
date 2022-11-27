package github.javaguide.registry.zk;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rpc.registry.zookeeper")
public class ZookeeperProperties {
    private String namespace;
    private String serverLists;
    private RetryPolicy retryPolicy = new RetryPolicy();
    private String digest;
    private Integer sessionTimeout = 30000;
    private Integer connectionTimeout = 10000;
    private Integer blockUntilConnected = 120;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getServerLists() {
        return serverLists;
    }

    public void setServerLists(String serverLists) {
        this.serverLists = serverLists;
    }

    public RetryPolicy getRetryPolicy() {
        return retryPolicy;
    }

    public void setRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    public String getDigest() {
        return digest;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public Integer getSessionTimeout() {
        return sessionTimeout;
    }

    public void setSessionTimeout(Integer sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public Integer getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public Integer getBlockUntilConnected() {
        return blockUntilConnected;
    }

    public void setBlockUntilConnected(Integer blockUntilConnected) {
        this.blockUntilConnected = blockUntilConnected;
    }

    public static final class RetryPolicy {
        private Integer baseSleepTime = 60000;
        private Integer maxRetries = 3;
        private Integer maxSleep = 30000;

        public Integer getBaseSleepTime() {
            return baseSleepTime;
        }

        public void setBaseSleepTime(Integer baseSleepTime) {
            this.baseSleepTime = baseSleepTime;
        }

        public Integer getMaxRetries() {
            return maxRetries;
        }

        public void setMaxRetries(Integer maxRetries) {
            this.maxRetries = maxRetries;
        }

        public Integer getMaxSleep() {
            return maxSleep;
        }

        public void setMaxSleep(Integer maxSleep) {
            this.maxSleep = maxSleep;
        }
    }
}