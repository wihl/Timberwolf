package com.softartisans.timberwolf.hbase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A proxy class for an HTable.
 */
public class HBaseTable implements IHBaseTable
{
    private static Logger logger = LoggerFactory.getLogger(HBaseTable.class);
    private HTableInterface table;
    private List<Put> puts = new ArrayList<Put>();
    private final String name;

    public HBaseTable(final HTableInterface hbaseTable)
    {
        this.table = hbaseTable;
        name = Bytes.toString(hbaseTable.getTableName());
    }

    /**
     * Adds a put to the underlying buffer to our HTable. It will not be added
     * to the HTable until flush is called.
     * @param put The Put to put to the underlying HTable.
     */
    @Override
    public final void put(final Put put)
    {
        puts.add(put);
    }

    @Override
    public final Result get(final Get get)
    {
        Result result = null;
        try
        {
            result = table.get(get);
        }
        catch (IOException e)
        {
            throw HBaseRuntimeException.create("Could not get from HBase!", e, logger);
        }
        return result;
    }

    /**
     * Batch processes all puts in the underlying buffer to a HTable.
     */
    @Override
    public final void flush()
    {
        try
        {
            table.put(puts);
            puts.clear();
        }
        catch (IOException e)
        {
            throw HBaseRuntimeException.create("Could not write puts to HTable!", e, logger);
        }

    }

    /**
     * Gets the name of the underlying HTable.
     * @return The name of the underlying HTable.
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * Closes the connection to the underlying table.
     */
    public final void close()
    {
        try
        {
            flush();
            table.close();
        }
        catch (IOException e)
        {
            throw HBaseRuntimeException.create("Could not close table " + name + "!", e, logger);
        }
    }
}
