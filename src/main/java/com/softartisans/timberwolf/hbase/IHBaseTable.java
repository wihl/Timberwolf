package com.softartisans.timberwolf.hbase;

import org.apache.hadoop.hbase.client.Put;

public interface IHBaseTable {
    void put(Put put);
    void flush();
    String getName();
}
