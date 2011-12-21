package com.softartisans.timberwolf.hbase;

import com.softartisans.timberwolf.MockHTable;
import org.junit.*;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import static org.mockito.Mockito.*;

/**
 * Unit tests for HBaseTable.
 */
public class HBaseTableTest
{
    /**
     * Default constructor test.
     */
    @Test
    public void testCreate()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
    }

    /**
     * Name test.
     */
    @Test
    public void testName()
    {
        String name = "defaultTableName";
        IHBaseTable table = new HBaseTable(MockHTable.create(name));
        Assert.assertEquals(name,table.getName());
    }

    /**
     * Put test.
     */
    @Test
    public void testPut()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
        Put put = mock(Put.class);
        table.put(put);
    }

    /**
     * Multiple puts test.
     */
    @Test
    public void testPuts()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
        for (int i = 0; i < 10; i++)
        {
            Put put = mock(Put.class);
            table.put(put);
        }
    }

    /**
     * Just flush test.
     */
    @Test
    public void testFlush()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
        table.flush();
    }

    /**
     * Flush with puts.
     */
    @Test
    public void testFlushWithPuts()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
        for (int i = 0; i < 10; i++)
        {
            // We currently need at least a row here or a null pointer exception
            // is thrown later in MockHTable.
            Put put = new Put(Bytes.toBytes("dummyRow"));
            table.put(put);
        }
        table.flush();
    }

}

