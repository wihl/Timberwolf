package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.conf.Configuration;
import org.junit.*;

public class HBaseConfiguratorTest
{
    /**
     * Simple test to actually make a non-default Hadoop configuration.
     */
    @Test
    public void testCreate()
    {
        String quorum = "hbase01.int.softartisans.com";
        String clientPort = "2181";
        Configuration configuration = HBaseConfigurator.createConfiguration(
                quorum,
                clientPort);
        Assert.assertEquals(
                quorum,configuration.get("hbase.zookeeper.quorum"));
        Assert.assertEquals(clientPort,configuration.get(
                "hbase.zookeeper.property.clientPort"));
    }
}
