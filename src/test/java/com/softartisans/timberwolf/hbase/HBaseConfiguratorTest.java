package com.softartisans.timberwolf.hbase;

import org.junit.Test;

public class HBaseConfiguratorTest {

    /**
     * Simple test to actually make a non-default Hadoop configuration.
     */
    @Test
    public void testCreate()
    {
        HBaseConfigurator.createConfiguration("/usr/local/hbase-0.90.4/hbase",
                "127.0.0.1:60000");
    }
}
