package com.ripariandata.timberwolf.hbase;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Test;

import com.ripariandata.timberwolf.MockHTable;
import com.ripariandata.timberwolf.hbase.HBaseManager;
import com.ripariandata.timberwolf.hbase.HBaseTable;
import com.ripariandata.timberwolf.hbase.IHBaseTable;

public class MockHTableTest
{
    private static final String TEST_COLUMN_FAMILY_STRING = "s";
    private static final byte[] SYNC_COLUMN_FAMILY = Bytes.toBytes(TEST_COLUMN_FAMILY_STRING);
    private static final byte[] SYNC_COLUMN_QUALIFIER = Bytes.toBytes("v");
    private static final String TEST_TABLE_NAME = "mockTable";
    
    private IHBaseTable mockTable(final HBaseManager hbaseManager)
    {
        MockHTable table = MockHTable.create(TEST_TABLE_NAME);
        HBaseTable hbaseTable = new HBaseTable(table);
        hbaseManager.addTable(hbaseTable);
        return hbaseTable;
    }
    
    public void setSyncState(IHBaseTable table,
                             final byte[] key,
                             final String value)
    {
        Put put = new Put(key);
        put.add(SYNC_COLUMN_FAMILY, SYNC_COLUMN_QUALIFIER,
        Bytes.toBytes(value));
        
        table.put(put);
        table.flush();
    }
    
    public String get(IHBaseTable table, final byte[] key)
    {
        Get get = new Get(key);
        Result result = table.get(get);
        Assert.assertFalse(result.isEmpty());
        return Bytes.toString(result.getValue(SYNC_COLUMN_FAMILY,
                                              SYNC_COLUMN_QUALIFIER));
    }
    
    @Test
    public void testResetValue()
    {
        HBaseManager manager = new HBaseManager();
        mockTable(manager);
        
        List<String> columnFamilies = new ArrayList<String>();
        columnFamilies.add(TEST_COLUMN_FAMILY_STRING);
        /** If the table already exists it will be simply grabbed not recreated. */
        IHBaseTable table = manager.createTable(TEST_TABLE_NAME, columnFamilies);

        final String value1 = "val1";
        final String value2 = "val2";
        final byte[] key = Bytes.toBytes("key");
        setSyncState(table, key, value1);
        Assert.assertEquals(value1, get(table, key));
        setSyncState(table, key, value2);
        Assert.assertEquals(value2, get(table, key));
    }
}
