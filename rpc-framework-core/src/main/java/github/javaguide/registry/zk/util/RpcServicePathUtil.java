/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package github.javaguide.registry.zk.util;

/**
 * Job node path.
 *
 * <p>
 * Job node is add job name as prefix.
 * </p>
 */
public final class RpcServicePathUtil {
    private static final String SERVERS_NODE = "servers";
    /**
     * Get full path.
     *
     * @return full path
     */
    public static String getFullPath(String rpcServiceName, String hostAndPort) {
        return String.format("/%s/%s/%s", rpcServiceName, SERVERS_NODE, hostAndPort);
    }

    /**
     * Get server node path.
     *
     * @return server node path
     */
    public static String getServerNodePath(String rpcServiceName) {
        return String.format("/%s/%s", rpcServiceName, SERVERS_NODE);
    }
}
