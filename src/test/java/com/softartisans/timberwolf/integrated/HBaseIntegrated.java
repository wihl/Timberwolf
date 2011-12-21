package com.softartisans.timberwolf.integrated;

import com.softartisans.timberwolf.hbase.HBaseManager;
import com.softartisans.timberwolf.hbase.IHBaseTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class HBaseIntegrated
{

    private static HBaseManager hBaseManager;
    private static final String tableName = "testTable";

    private static void createTable(String tableName)
    {
        List<String> cfs = new ArrayList<String>();
        cfs.add("cf");
        hBaseManager.createTable(tableName,cfs);
        Assert.assertTrue(hBaseManager.tableExists(tableName));
    }

    private static void deleteTable(String tableName)
    {
        hBaseManager.deleteTable(tableName);
        Assert.assertFalse(hBaseManager.tableExists(tableName));
    }

    /**
     * Fixture setup.
     */
    @BeforeClass
    public static void setUp()
    {
        hBaseManager = new HBaseManager(
                IntegrationSettings.ZooKeeperQuorum,
                IntegrationSettings.ZooKeeperClientPort);
    }

    /**
     * Fixture tear down.
     */
    @AfterClass
    public static void tearDown()
    {
        hBaseManager.close();
    }

    /**
     * Tests that we can create and delete a table on the remote HBase instance.
     */
    @Test
    public void testRemoteConnection()
    {
        String tableName = "HBaseIntegratedtestRemoteConnection";

        createTable(tableName);
        deleteTable(tableName);
    }

    /**
     * Tests getting a remote table instance.
     */
    @Test
    public void testRemoteGetTable()
    {
        String tableName = "HBaseIntegratedtestRemoteGetTable";

        createTable(tableName);

        IHBaseTable table = hBaseManager.getTable(tableName);
        Assert.assertEquals(tableName, table.getName());
        table.close();

        deleteTable(tableName);
    }

}
