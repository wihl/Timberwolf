package com.softartisans.timberwolf.integrated;

import com.softartisans.timberwolf.PropertiesForTests;
import com.softartisans.timberwolf.hbase.HBaseManager;
import com.softartisans.timberwolf.hbase.IHBaseTable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.ArrayList;
import java.util.List;

public class TestHBaseIntegration
{
    @Rule
    public TestName name = new TestName();
    private static HBaseManager hBaseManager;
    private static final String tableName = "testTable";
    private static final String ZOO_KEEPER_QUORUM_PROPERTY_NAME = "ZooKeeperQuorum";
    private static final String ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME = "ZooKeeperClientPort";

    private static void createTable(String tableName)
    {
        List<String> cfs = new ArrayList<String>();
        cfs.add("cf");
        hBaseManager.createTable(tableName, cfs);
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
    public static void classSetUp()
    {
        String zooKeeperQuorum = PropertiesForTests.getProperty(ZOO_KEEPER_QUORUM_PROPERTY_NAME);
        String zooKeeperClientPort = PropertiesForTests.getProperty(ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME);
        if (zooKeeperQuorum != null && zooKeeperClientPort != null)
        {
            hBaseManager = new HBaseManager(zooKeeperQuorum, zooKeeperClientPort);
        }
    }

    @Before
    public void setUp()
    {
        PropertiesForTests.assume(name, ZOO_KEEPER_QUORUM_PROPERTY_NAME, ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME);
    }

    /**
     * Fixture tear down.
     */
    @AfterClass
    public static void tearDown()
    {
        if (hBaseManager != null)
        {
            hBaseManager.close();
        }
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

        IHBaseTable table = hBaseManager.getTable(tableName);
        Assert.assertEquals(tableName, table.getName());
        table.close();

        deleteTable(tableName);
    }

}
