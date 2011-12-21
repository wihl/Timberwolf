package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;

/**
 * An interface for HBase table proxies.
 */
public interface IHBaseTable
{
    void put(Put put);
    void flush();
    String getName();
    void close();
}
