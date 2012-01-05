package com.softartisans.timberwolf.integrated;

import com.softartisans.timberwolf.hbase.HBaseConfigurator;
import com.softartisans.timberwolf.hbase.HBaseManager;
import com.softartisans.timberwolf.hbase.IHBaseTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.HTable;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

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
    private String name;
    private HBaseManager hbaseManager;
    private IHBaseTable table;

    /** Create a new htable resource */
    public HTableResource()
    {
        super(ZOO_KEEPER_QUORUM_PROPERTY_NAME, ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME);
    }

    @Override
    public Statement apply(Statement base, final Description description)
    {
        name = description.getClassName() + "." + description.getMethodName()
               + (new BigInteger(130, new Random()).toString(32));
        LOG.debug("Using temporary table: " + name);
        final Statement inner = super.apply(base, description);
        return new Statement()
        {
            @Override
            public void evaluate() throws Throwable
            {
                hbaseManager = new HBaseManager(getProperty(ZOO_KEEPER_QUORUM_PROPERTY_NAME),
                                                getProperty(ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME));

                List<String> columnFamilies = new ArrayList<String>();
                columnFamilies.add(COLUMN_FAMILY);
                table = hbaseManager.createTable(name, columnFamilies);
                try
                {
                    inner.evaluate();
                }
                finally
                {
                    hbaseManager.deleteTable(name);
                }
            }
        };
    }

    /**
     * The column family for the table.
     * @return the column family for the created table
     */
    public String getFamily()
    {
        return COLUMN_FAMILY;
    }

    /**
     * The table as created by our production code.
     * @return the table created by our production code
     */
    public IHBaseTable getTable()
    {
        return table;
    }

    /**
     * The table created independently of our production code.
     * You should use this table to confirm the contents of the table
     * @return the table used for testing
     * @throws IOException if there was an error creating the table object
     */
    public HTable getTestingTable() throws IOException
    {
        Configuration configuration = HBaseConfigurator.createConfiguration(
                getProperty(ZOO_KEEPER_QUORUM_PROPERTY_NAME),
                getProperty(ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME));
        return new HTable(configuration, name);
    }

}
