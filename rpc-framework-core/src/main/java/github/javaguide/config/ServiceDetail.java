package github.javaguide.config;

import com.google.common.base.Strings;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author shuang.kou
 * @createTime 2020年07月21日 20:23:00
 **/
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class ServiceDetail {
    /**
     * service version
     */
    private String version = "";
    /**
     * when the interface has multiple implementation classes, distinguish by group
     */
    private String group = "";

    private String serviceName;

    private String rpcServiceName;

    private Object serviceObj;

    private int timeout;

    private int retries;

    public String getRpcServiceName() {
        if (Strings.isNullOrEmpty(this.rpcServiceName)) {
            String groupPath = Strings.isNullOrEmpty(this.group) ? "" : this.group + "/";
            this.rpcServiceName = groupPath + this.getVersion() + "/" + this.getServiceName();
        }
        return this.rpcServiceName;
    }
}
