package com.ripariandata.timberwolf.integrated;

import com.ripariandata.timberwolf.hbase.IHBaseTable;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

public class TestHBaseIntegration
{
    @Rule
    public HTableResource htable = new HTableResource();

    private static Put createPut(String rowKey, String family, String qualifier,
                                 String value)
    {
        Put put = new Put(Bytes.toBytes(rowKey));
        put.add(Bytes.toBytes(family),
                Bytes.toBytes(qualifier),
                Bytes.toBytes(value));
        return put;
    }

    private static Get createGet(String rowKey, String family)
    {
        Get get = new Get(Bytes.toBytes(rowKey));
        get.addFamily(Bytes.toBytes(family));
        return get;
    }

    /**
     * Tests that we can create and delete a table on the remote HBase instance.
     */
    @Test
    public void testRemoteCreateTable()
    {
        Assert.assertTrue(htable.exists());
    }

    /**
     * Tests getting a remote table instance.
     */
    @Test
    public void testRemoteGetTable() throws IOException
    {
        String tableName = htable.getName();
        IHBaseTable table = htable.regetTable();
        Assert.assertEquals(tableName, table.getName());
    }

    /**
     *  Tests a remote put operation and compares the values from an HBase
     *  get operation.
     */
    @Test
    public void testRemotePut()
    {
        String rowKey = "aGenericRowKey";
        String qualifier = "aGenericQualifier";
        String value = "someValue";

        IHBaseTable table = htable.getTable();

        Put put = createPut(rowKey, htable.getFamily(), qualifier,value);
        table.put(put);

        try
        {
            // We want to do the read without using the manager.
            HTableInterface tableInterface = htable.getTestingTable();

            Result result = tableInterface.get(createGet(rowKey,htable.getFamily()));
            String tableValue = Bytes.toString(result.getValue(Bytes.toBytes(htable.getFamily()),
                                                               Bytes.toBytes(qualifier)));
            Assert.assertEquals(value,tableValue);
        }
        catch (IOException e)
        {
            Assert.fail("Exception getting our record: " + e.getMessage());
        }
    }

}
