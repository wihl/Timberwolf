package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

/**
 * Factory class for creating HBase-compatible Hadoop Configurations from
 * specified elements.
 */
public abstract class HBaseConfigurator
{

    /**
     * The property name for the ZooKeeper quorum property.
     */
    private static final String ZK_QUORUM = "hbase.zookeeper.quorum";

    /**
     * The property name of the ZooKeeper client port property.
     */
    private static final String ZK_CLIENT_PORT =
            "hbase.zookeeper.property.clientPort";

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
        configuration.set(ZK_QUORUM, quorum);
        configuration.set(ZK_CLIENT_PORT, clientPort);
        return configuration;
    }
}
