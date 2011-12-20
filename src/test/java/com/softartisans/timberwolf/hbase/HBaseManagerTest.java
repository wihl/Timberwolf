package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.*;

/**
 * Unit test for HBaseManager.
 */
public class HBaseManagerTest
{
    /**
     * Our logger for this class.
     */
    Logger logger = LoggerFactory.getLogger(HBaseManagerTest.class);
    private static HBaseManager hBaseManager;
    private static final String tableName = "testTable";
    private static final String defaultRootDir =
            "/usr/local/hbase-0.90.4/hbase";
    private static final String defaultMaster = "127.0.0.1:60000";
    private static List<String> columnFamilies;

    /**
     * Creates an IHBaseTable with a specific name.
     * @param name The name of the IHBaseTable.
     * @return A IHBaseTable with a specific name.
     */
    private IHBaseTable createNamedTable(String name)
    {
        HBaseTable table = mock(HBaseTable.class);
        when(table.getName()).thenReturn(name);
        return table;
    }

    /**
     * Fixture setup.
     */
    @BeforeClass
    public static void setUp()
    {
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
//        hBaseManager.close();
    }

    /**
     * Create test.
     */
    @Test
    public void testCreate()
    {
        HBaseManager hbase = new HBaseManager();
    }

    /**
     * Test that we can simply add.
     */
    @Test
    public void testAdd()
    {
        HBaseManager hbase = new HBaseManager();
        IHBaseTable table = mock(HBaseTable.class);
        hbase.addTable(table);
    }

    /**
     * Test that we can get a table, and it is the same
     * as the one put in.
     */
    @Test
    public void testGetFromAdd()
    {
        String tableName = "defaultTableName";
        HBaseManager hbase = new HBaseManager();
        IHBaseTable table = createNamedTable(tableName);
        hbase.addTable(table);
        IHBaseTable managerTable = hbase.getTable(tableName);
        Assert.assertEquals(table, managerTable);
        Assert.assertEquals(tableName, managerTable.getName());
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
     * TODO: Make generic.
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
     * Simple HBase connection to a remote network server.
     */
    @Test
    public void testRemoteConnection()
    {
        HBaseManager hbase = new HBaseManager("hdhbase01.int.softartisans.com",
                "45000");
        List<String> cfs = new ArrayList<String>();
        cfs.add("cf");
        hbase.createTable("testTable",cfs);
        hbase.deleteTable("testTable");
    }


}
