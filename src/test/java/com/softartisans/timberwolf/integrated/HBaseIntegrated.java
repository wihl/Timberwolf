package com.softartisans.timberwolf.integrated;

import com.softartisans.timberwolf.hbase.HBaseConfigurator;
import com.softartisans.timberwolf.hbase.HBaseManager;
import com.softartisans.timberwolf.hbase.IHBaseTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class HBaseIntegrated
{

    private static HBaseManager hBaseManager;
    private static final String defaultColumnFamily = "h";

    private static void createTable(String tableName)
    {
        List<String> cfs = new ArrayList<String>();
        cfs.add(defaultColumnFamily);
        hBaseManager.createTable(tableName,cfs);
        Assert.assertTrue(hBaseManager.tableExists(tableName));
    }

    private static void deleteTable(String tableName)
    {
        hBaseManager.deleteTable(tableName);
        Assert.assertFalse(hBaseManager.tableExists(tableName));
    }

    private static IHBaseTable getTable(String tableName)
    {
        IHBaseTable table = hBaseManager.getTable(tableName);
        Assert.assertEquals(tableName, table.getName());
        return table;
    }

    private static Put createPut(String rowKey, String family, String qualifier,
                                 String value)
    {
        Put put = new Put(Bytes.toBytes(rowKey));
        put.add(Bytes.toBytes(family),
                Bytes.toBytes(qualifier),
                Bytes.toBytes(value));
        return put;
    }

    private static Get createGet(String rowKey, String family)
    {
        Get get = new Get(Bytes.toBytes(rowKey));
        get.addFamily(Bytes.toBytes(family));
        return get;
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
    public void testRemoteCreateDeleteTable()
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
        IHBaseTable table = getTable(tableName);
        table.close();

        deleteTable(tableName);
    }

    /**
     *  Tests a remote put operation and compares the values from an HBase
     *  get operation.
     */
    @Test
    public void testRemotePut()
    {
        String tableName = "HBaseIntegratedtestRemotePut";
        String rowKey = "aGenericRowKey";
        String qualifier = "aGenericQualifier";
        String value = "someValue";

        createTable(tableName);
        IHBaseTable table = getTable(tableName);

        Put put = createPut(rowKey,defaultColumnFamily,
                qualifier,value);
        table.put(put);
        table.close();

        // We want to do the read without using the manager.
        Configuration configuration =
                HBaseConfigurator.createConfiguration(
                        IntegrationSettings.ZooKeeperQuorum,
                        IntegrationSettings.ZooKeeperClientPort);
        try
        {
            HTableInterface tableInterface = new HTable(configuration,
                    tableName);

            Result result = tableInterface.get(createGet(rowKey,
                    defaultColumnFamily));
            String tableValue = Bytes.toString(result.getValue(
                    Bytes.toBytes(defaultColumnFamily),
                    Bytes.toBytes(qualifier)));
            Assert.assertEquals(value,tableValue);
        }
        catch (IOException e)
        {
            Assert.fail("Exception getting our record: " + e.getMessage());
        }

        deleteTable(tableName);
    }

}
