package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class HBaseTable implements IHBaseTable
{
    private static Logger logger = LoggerFactory.getLogger(HBaseTable.class);
    private HTable table;
    private List<Put> puts;

    public HBaseTable(HTable table)
    {
        this.table = table;
    }

    @Override
    public void put(Put put)
    {
        puts.add(put);
    }

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
}
