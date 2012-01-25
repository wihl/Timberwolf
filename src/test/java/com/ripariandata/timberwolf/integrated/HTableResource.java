package com.ripariandata.timberwolf.integrated;

import com.ripariandata.timberwolf.hbase.HBaseConfigurator;
import com.ripariandata.timberwolf.hbase.HBaseManager;
import com.ripariandata.timberwolf.hbase.IHBaseTable;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is an external resource for managing an HTable.
 * It creates the htable before the test starts, and then creates another
 * class for accessing the same table for testing, when you call
 * getTestingTable.
 * <br/>
 * This resource creates a table based on the test name and a random string
 * to avoid conflicts with anyone else who may be sharing the hbase cluster.
 */
public class HTableResource extends IntegrationTestProperties
{

    private static final Logger LOG = LoggerFactory.getLogger(HTableResource.class);
    private static final String ZOO_KEEPER_QUORUM_PROPERTY_NAME = "ZooKeeperQuorum";
    private static final String ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME = "ZooKeeperClientPort";
    private static final String COLUMN_FAMILY = "h";
    /** A lock for safely creating the hbaseManager. */
    private static final Object MANAGER_LOCK = new Object();
    /** The hbaseManager shared among HTableResources. */
    private static HBaseManager hbaseManager;
    private String name;
    private IHBaseTable table;
    private HTable testingTable;
    private String columnFamily;

    /** Create a new htable resource. */
    public HTableResource()
    {
        this(COLUMN_FAMILY);
    }

    public HTableResource(final String columnFamilyName)
    {
        super(ZOO_KEEPER_QUORUM_PROPERTY_NAME, ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME);
        columnFamily = columnFamilyName;
    }

    @Override
    public Statement apply(final Statement inner, final Description description)
    {
        final int arbitraryValue1 = 130;
        final int arbitraryValue2 = 32;
        name = description.getClassName() + "." + description.getMethodName()
               + (new BigInteger(arbitraryValue1, new Random()).toString(arbitraryValue2));
        LOG.debug("Using temporary table: " + name);
        return super.apply(new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                synchronized (MANAGER_LOCK)
                {
                    if (hbaseManager == null)
                    {
                        hbaseManager = new HBaseManager(getProperty(ZOO_KEEPER_QUORUM_PROPERTY_NAME),
                                                        getProperty(ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME));
                    }
                }

                table = createTable();
                try
                {
                    inner.evaluate();
                }
                finally
                {
                    try
                    {
                        closeTables();
                    }
                    finally
                    {
                        try
                        {
                            hbaseManager.deleteTable(name);
                        }
                        finally
                        {
                            hbaseManager.close();
                        }
                    }
                }
            }
        }, description);
    }

    void closeTables() throws IOException
    {
        try
        {
            if (table != null)
            {
                hbaseManager.close();
                table = null;
            }
        }
        finally
        {
            if (testingTable != null)
            {
                testingTable.close();
                testingTable = null;
            }
        }
    }

    /**
     * The column family for the table.
     *
     * @return the column family for the created table
     */
    public String getFamily()
    {
        return COLUMN_FAMILY;
    }

    /**
     * The table as created by ice.submit(AbstractExour production code.
     *
     * @return the table created by our production code
     */
    public IHBaseTable getTable()
    {
        return table;
    }

    /**
     * Closes the current table and regets the table from hbase.
     *
     * @return a new instance of the table
     * @throws IOException if there was a problem closing or getting the table
     */
    public IHBaseTable regetTable() throws IOException
    {
        closeTables();
        table = hbaseManager.getTable(name);
        return table;
    }

    /**
     * Creates a randomly named table for testing.
     * <b>Note:</b> This won't create multiple tables if called more than once.
     * To have multiple tables, use multiple HTableResources
     *
     * @return the table created
     */
    private IHBaseTable createTable()
    {
        if (table == null)
        {
            List<String> columnFamilies = new ArrayList<String>();
            columnFamilies.add(columnFamily);
            table = hbaseManager.createTable(name, columnFamilies);
        }
        return table;
    }

    /**
     * returns whether or not the table exists.
     *
     * @return true if the table exists
     */
    public boolean exists()
    {
        return hbaseManager.tableExists(getName());
    }

    /**
     * Returns the name of the table.
     *
     * @return the name of the table
     */
    public String getName()
    {
        return name;
    }

    /**
     * The table created independently of our production code.
     * You should use this table to confirm the contents of the table.
     * Calling this method will close and nullify the regular table,
     * because hbase client doesn't handle multiple references to the
     * same table.
     * <br/>
     * <p/>
     * <b>NOTE:</b>You are responsible for closing the returned htable.
     *
     * @return the table used for testing
     * @throws IOException if there was an error creating the table object
     */
    public HTable getTestingTable() throws IOException
    {
        closeTables();
        Configuration configuration = HBaseConfigurator.createConfiguration(
                getProperty(ZOO_KEEPER_QUORUM_PROPERTY_NAME),
                getProperty(ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME));
        testingTable = new HTable(configuration, name);
        return testingTable;
    }

    /**
     * Gets the hbaseManager used.
     *
     * @return the hbase manager that is managing tables for the tests
     */
    public HBaseManager getManager()
    {
        return hbaseManager;
    }
}
