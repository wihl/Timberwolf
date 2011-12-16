package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HColumnDescriptor;
import org.apache.hadoop.hbase.HTableDescriptor;
import org.apache.hadoop.hbase.MasterNotRunningException;
import org.apache.hadoop.hbase.ZooKeeperConnectionException;
import org.apache.hadoop.hbase.client.HBaseAdmin;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages the set of HBaseTables currently in use.
 */
public class HBaseManager
{

    /**
     * The logger for this class.
     */
    private static Logger logger = LoggerFactory.getLogger(HBaseManager.class);

    /**
     * The underlying table collection.
     */
    private Map<String, IHBaseTable> tables =
            new HashMap<String, IHBaseTable>();

    /**
     * The underlying remote configuration.
     */
    private Configuration configuration;

    /**
     * The remote HBase instance.
     */
    private HBaseAdmin hbase;

    /**
     * Whether or not this HBase manager SHOULD connect remotely.
     */
    private boolean connectRemotely = true;

    /**
     * Constructor for creating a simple manager. Tables must be manually added.
     */
    public HBaseManager()
    {
        connectRemotely = false;
    }

    /**
     * Constructor for creating a manager for the default HBase configuration.
     */
    public HBaseManager(final Configuration hbaseConfiguration)
    {
        configuration = hbaseConfiguration;

        try
        {
            hbase = new HBaseAdmin(configuration);
        }
        catch (MasterNotRunningException e)
        {
            logger.error("Unable to connect to Master!");
        }
        catch (ZooKeeperConnectionException e)
        {
            logger.error("Unable to connect to ZooKeeper!");
        }
    }

    /**
     * Constructor for creating a manager for a specific HBase instance.
     * @param rootDir The directory shared by the HBase region servers.
     * @param master The host and port number that the HBase master runs at.
     */
    public HBaseManager(final String rootDir, final String master)
    {
        this(HBaseConfigurator.createConfiguration(rootDir, master));
    }

    /**
     * Adds a HBaseTable to the underlying tables collection. If the table
     * already exists, it will do nothing.
     * @param table The IHBaseTable to add.
     */
    public final void addTable(final IHBaseTable table)
    {
        if (!tables.containsValue(table))
        {
            tables.put(table.getName(), table);
        }
    }

    /**
     * Gets a table by table name from HBase or from the underlying collection,
     * to prevent multiple references to the same HTable.
     * @param tableName The name of the HTable to get.
     * @return The IHBaseTable for this table name.
     */
    public final IHBaseTable getTable(final String tableName)
    {
        if (tables.containsKey(tableName))
        {
            return tables.get(tableName);
        }
        IHBaseTable table = getTableRemotely(tableName);
        if (table != null)
        {
            addTable(table);
        }
        return table;
    }

    /**
     * Determines whether or not a given table currently exists.
     * @param tableName The name of the table.
     * @return Whether or not the specified table exists.
     */
    public final boolean tableExists(final String tableName)
    {
        if (tables.containsKey(tableName) || tableExistsRemotely(tableName))
        {
            return true;
        }
        return false;
    }

    /**
     * Determines whether or not a table exists on the remote instance.
     * @param tableName The name of the table.
     * @return Whether or not the table exists remotely.
     */
    private boolean tableExistsRemotely(final String tableName)
    {
        if (canRemote())
        {
            try
            {
                return hbase.tableExists(tableName);
            }
            catch (IOException e)
            {
                logger.error("Could not determine existence of table "
                        + tableName + "!");
            }
        }
        return false;
    }

    /**
     * Acquires an IHBaseTable for a remote table instance.
     * @param tableName The name of the table.
     * @return An IHBaseTable for a remote table instance.
     */
    private IHBaseTable getTableRemotely(final String tableName)
    {
        if (canRemote())
        {
            if (tableExistsRemotely(tableName))
            {
                logger.info("Table " + tableName + " exists.");
                HTableInterface table;
                try
                {
                    table = new HTable(tableName);
                    return new HBaseTable(table);
                }
                catch (IOException e)
                {
                    logger.error("Could not acquire reference to table "
                            + tableName + "!");
                }
            }
            else
            {
                logger.info("Table " + tableName + " does not exist.");
            }
        }
        return null;
    }

    /**
     * Determines whether or not this HBaseManager can call a remote instance.
     * @return Whether or not this HBaseManager can call a remote instance.
     */
    private boolean canRemote()
    {
        if (!connectRemotely)
        {
            return false;
        }
        if (hbase == null)
        {
            logger.error("HBase instance is not initialized!");
        }
        return true;
    }

    /**
     * Creates a table with the given name and list of column family names. It
     * will be added to the underlying table collection. If the table already
     * exists, a warning will be logged and the table will be added to the
     * underlying collection.
     * @param tableName The name of the table.
     * @param columnFamilies A list of column family names.
     */
    public final void createTable(final String tableName,
                                  final List<String> columnFamilies)
    {
        if (canRemote())
        {
            HTableDescriptor tableDescriptor = new HTableDescriptor(tableName);
            for (String columnFamily : columnFamilies)
            {
                HColumnDescriptor columnDescriptor =
                        new HColumnDescriptor(columnFamily);
                tableDescriptor.addFamily(columnDescriptor);
            }
            try
            {
                if (hbase.tableExists(tableName))
                {
                    logger.error("Cannot create table " + tableName + ", as "
                        + "the table already exists!");
                }
                else
                {
                    hbase.createTable(tableDescriptor);
                }
                HTableInterface table = new HTable(tableName);
                addTable(new HBaseTable(table));
            }
            catch (IOException e)
            {
                logger.error("Error creating table " + tableName + "!");
            }
        }
    }

    /**
     * Deletes a table with the given name. If remotely connected, will
     * delete the table from HBase.
     * @param tableName The name of the table to delete.
     */
    public final void deleteTable(final String tableName)
    {
        if (canRemote())
        {
            try
            {
                hbase.disableTable(tableName);
                hbase.deleteTable(tableName);
            }
            catch (IOException e)
            {
                logger.error("Error deleting table " + tableName + "!");
            }
        }
        tables.remove(tableName);
    }

    /**
     * Closes the connections for all managed tables and clears
     * the underlying table collection.
     */
    public final void close()
    {
        for (IHBaseTable table : tables.values())
        {
            table.close();
        }
        tables.clear();
    }
}
