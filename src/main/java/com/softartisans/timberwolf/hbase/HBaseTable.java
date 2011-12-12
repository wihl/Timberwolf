package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * A proxy class for an HTable.
 */
public class HBaseTable implements IHBaseTable
{
    private static Logger logger = LoggerFactory.getLogger(HBaseTable.class);
    private HTable table;
    private List<Put> puts;
    private final String name;

    protected HBaseTable(HTable table)
    {
        this.table = table;
        name = Bytes.toString(table.getTableName());
    }

    /**
     * Adds a put to the underlying buffer to our HTable. It will not be added
     * to the HTable until flush is called.
     * @param put
     */
    @Override
    public void put(Put put)
    {
        puts.add(put);
    }

    /**
     * Batch processes all puts in the underlying buffer to a HTable.
     */
    @Override
    public void flush()
    {
        try
        {
            table.put(puts);
            puts.clear();
        } catch (IOException e) {
            logger.error("Could not write puts to HTable!");
        }

    }

    /**
     * Gets the name of the underlying HTable.
     * @return The name of the underlying HTable.
     */
    @Override
    public String getName() {
        return name;
    }
}
