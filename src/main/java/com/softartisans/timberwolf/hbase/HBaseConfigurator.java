package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HConstants;
import org.apache.hadoop.hbase.HBaseConfiguration;

/**
 * Factory class for creating HBase-compatible Hadoop Configurations from
 * specified elements.
 */
public abstract class HBaseConfigurator
{
    /**
     * Creates a Hadoop Configuration for HBase using the specified
     * properties.
     * @param quorum The ZooKeeper quorum members.
     * @param clientPort The ZooKeeper client port.
     * @return A Hadoop Configuration object with the above parameters.
     */
    public static final Configuration createConfiguration(final String quorum,
                                                        final String clientPort)
    {
        Configuration configuration = HBaseConfiguration.create();
        configuration.set(HConstants.ZOOKEEPER_QUORUM, quorum);
        configuration.set(HConstants.ZOOKEEPER_CLIENT_PORT, clientPort);
        return configuration;
    }
}
