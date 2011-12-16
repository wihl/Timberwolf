package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;

/**
 * Factory class for creating HBase-compatible Hadoop Configurations from
 * specified elements.
 */
public abstract class HBaseConfigurator {

    /**
     * The property name for the rootdir property.
     */
    private static final String HBASE_ROOTDIR = "hbase.rootdir";

    /**
     * The property name of the master property.
     */
    private static final String HBASE_MASTER = "hbase.master";

    /**
     * Creates a Hadoop Configuration for HBase using the specified
     * properties.
     * @param rootDir The directory shared by the HBase region servers.
     * @param master The host and port number that the HBase master runs at.
     * @return A Hadoop Configuration object with the above parameters.
     */
    public static final Configuration createConfiguration(final String rootDir,
                                                          final String master)
    {
        Configuration configuration = HBaseConfiguration.create();
        configuration.set(HBASE_ROOTDIR, rootDir);
        configuration.set(HBASE_MASTER, master);
        return configuration;
    }
}
