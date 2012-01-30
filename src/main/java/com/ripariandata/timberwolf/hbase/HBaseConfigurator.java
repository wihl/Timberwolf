/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ripariandata.timberwolf.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.HConstants;

/**
 * Factory class for creating HBase-compatible Hadoop Configurations from
 * specified elements.
 */
public abstract class HBaseConfigurator
{
    /** The property name of the ZooKeeper client port property. */
    private static final String ZOOKEEPER_CLIENT_PORT = "hbase.zookeeper.property.clientPort";

    /**
     * Creates a Hadoop Configuration for HBase using the specified
     * properties.
     *
     * @param quorum The ZooKeeper quorum members.
     * @param clientPort The ZooKeeper client port.
     * @return A Hadoop Configuration object with the above parameters.
     */
    public static final Configuration createConfiguration(final String quorum, final String clientPort)
    {
        Configuration configuration = HBaseConfiguration.create();
        configuration.set(HConstants.ZOOKEEPER_QUORUM, quorum);
        configuration.set(ZOOKEEPER_CLIENT_PORT, clientPort);
        return configuration;
    }
}
