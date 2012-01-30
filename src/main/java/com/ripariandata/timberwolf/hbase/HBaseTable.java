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
package com.ripariandata.timberwolf.hbase;

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
 * This class should only be referenced and used by HBaseManager.
 */
class HBaseTable implements IHBaseTable
{
    private static final Logger LOG = LoggerFactory.getLogger(HBaseTable.class);
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
            throw HBaseRuntimeException.log(LOG, new HBaseRuntimeException("Could not get from HBase!", e));
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
            throw HBaseRuntimeException.log(LOG, new HBaseRuntimeException("Could not write puts to HTable!", e));
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
     * This should only be called by HBaseManager.close().
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
            throw HBaseRuntimeException.log(LOG, new HBaseRuntimeException("Could not close table " + name + "!", e));
        }
    }
}
