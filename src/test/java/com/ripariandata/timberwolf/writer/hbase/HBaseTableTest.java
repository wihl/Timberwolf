/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ripariandata.timberwolf.writer.hbase;

import com.ripariandata.timberwolf.MockHTable;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Test;

import static org.mockito.Mockito.mock;

/**
 * Unit tests for HBaseTable.
 */
public class HBaseTableTest
{
    private final String defaultColumnFamily = "f";
    private final String defaultColumnQualifier = "q";
    private static final int DEFAULT_PUT_COUNT = 10;

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
        Assert.assertEquals(name, table.getName());
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
        for (int i = 0; i < DEFAULT_PUT_COUNT; i++)
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
        for (int i = 0; i < DEFAULT_PUT_COUNT; i++)
        {
            // We currently need at least a row here or a null pointer exception
            // is thrown later in MockHTable.
            Put put = new Put(Bytes.toBytes("dummyRow"));
            table.put(put);
        }
        table.flush();
    }

    /**
     * Simple get.
     */
    @Test
    public void testGet()
    {
        String rowKey = "row";
        IHBaseTable table = new HBaseTable(MockHTable.create());
        final int count = 10;
        for (int i = 0; i < count; i++)
        {
            Put put = new Put(Bytes.toBytes(rowKey + i));
            put.add(Bytes.toBytes(defaultColumnFamily), Bytes.toBytes(defaultColumnQualifier), Bytes.toBytes(i));
            table.put(put);
        }
        table.flush();

        for (int i = 0; i < count; i++)
        {
            Get get = new Get(Bytes.toBytes(rowKey + i));
            Result result = table.get(get);
            int value = Bytes.toInt(result.getValue(Bytes.toBytes(defaultColumnFamily),
                    Bytes.toBytes(defaultColumnQualifier)));
            Assert.assertEquals(i, value);
        }
    }

    /**
     * Get with no result.
     */
    @Test
    public void testGetNoResult()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());

        Get get = new Get(Bytes.toBytes("totallyArbitraryRowKey"));
        Result result = table.get(get);
        Assert.assertTrue(result.isEmpty());
    }

}

