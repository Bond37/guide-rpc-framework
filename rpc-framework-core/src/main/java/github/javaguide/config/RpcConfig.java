package github.javaguide.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author bond
 * @description
 * @date 2022/11/25
 */
@Data
@ConfigurationProperties(prefix = "rpc.protocol")
public class RpcConfig {

    private int port;

    private int retries = 3;
    /** 单位毫秒*/
    private int timeout = 6000;

    private boolean provider;

    private boolean consumer;
}
