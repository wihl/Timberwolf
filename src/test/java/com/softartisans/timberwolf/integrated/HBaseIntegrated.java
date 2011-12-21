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
    private static final String defaultRootDir =
            "/usr/local/hbase-0.90.4/hbase";
    private static final String defaultMaster = "127.0.0.1:60000";
    private static List<String> columnFamilies;

    /**
     * Fixture setup.
     */
    @BeforeClass
    public static void setUp()
    {
        hBaseManager = new HBaseManager(
                IntegrationSettings.ZooKeeperQuorum,
                IntegrationSettings.ZooKeeperClientPort);

//        Configuration configuration = HBaseConfiguration.create();
//        hBaseManager = new HBaseManager(configuration);
//
//        columnFamilies = new ArrayList<String>();
//        columnFamilies.add("h");
//
//        hBaseManager.createTable(tableName, columnFamilies);
    }

    /**
     * Fixture tear down.
     */
    @AfterClass
    public static void tearDown()
    {
//        hBaseManager.deleteTable(tableName);
        hBaseManager.close();
    }

    /**
     * Tests getting a remote table instance.
     */
    @Test
    public void testGetTableRemotely()
    {
        IHBaseTable table = hBaseManager.getTable(tableName);
        Assert.assertEquals(tableName, table.getName());
    }

    /**
     * Tests creating and deleting a table remotely.
     */
    @Test
    public void testCreateAndDeleteTable()
    {
        String table = "aNewTable";
        List<String> columnFamilies = new ArrayList<String>();
        columnFamilies.add("cf");
        hBaseManager.createTable(table, columnFamilies);
        hBaseManager.deleteTable(table);
    }

    /**
     * Simple test to show that we can make a HBaseManager for a specific HBase
     * instance.
     */
    @Test
    public void testCreateSpecificRemote()
    {
        HBaseManager hbase = new HBaseManager(defaultRootDir, defaultMaster);
    }

    /**
     * Tests that we can perform actions on a specific HBase table.
     */
    @Test
    public void testCreateTableSpecificRemote()
    {
        String aTable = "aTempTable";
        HBaseManager hbase = new HBaseManager(defaultRootDir, defaultMaster);
        hbase.createTable(aTable, columnFamilies);
        hbase.deleteTable(aTable);
    }

    /**
     * Tests that we can create and delete a table on the remote HBase instance.
     */
    @Test
    public void testRemoteConnection()
    {
        String tableName = "testTable";

        List<String> cfs = new ArrayList<String>();
        cfs.add("cf");
        hBaseManager.createTable(tableName,cfs);
        Assert.assertTrue(hBaseManager.tableExists(tableName));
        hBaseManager.deleteTable(tableName);
        Assert.assertFalse(hBaseManager.tableExists(tableName));
    }
}
