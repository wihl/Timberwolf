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

import com.ripariandata.timberwolf.MockHTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

/** Tests the behavior of HBaseUserTimeUpdaters. */
public class HBaseUserTimeUpdaterTest
{
    private HBaseManager manager = new HBaseManager();

    private IHBaseTable mockTable(final HBaseManager hbaseManager, final String tableName)
    {
        MockHTable table = MockHTable.create(tableName);
        HBaseTable hbaseTable = new HBaseTable(table);
        hbaseManager.addTable(hbaseTable);
        return hbaseTable;
    }

    @Test
    public void testLastUpdated()
    {
        String tableName = "testLastUpdated";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        String userName = "Robert the User";
        Put put = new Put(Bytes.toBytes(userName));
        final long time = 23488902348L;
        put.add(Bytes.toBytes("t"), Bytes.toBytes("d"), Bytes.toBytes(time));
        hbaseTable.put(put);
        hbaseTable.flush();

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, tableName);
        DateTime date = updates.lastUpdated(userName);
        Assert.assertEquals(time, date.getMillis());
    }

    @Test
    public void testLastUpdatedNoUser()
    {
        String tableName = "testLastUpdatedNoUser";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, tableName);
        DateTime date = updates.lastUpdated("not actually a username");
        Assert.assertEquals(0L, date.getMillis());
    }

    @Test
    public void testUpdateUser()
    {
        String tableName = "testUpdateUser";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, tableName);
        final long time = 1234355L;
        String userName = "A Generic Username";
        updates.setUpdateTime(userName, new DateTime(time));
        Assert.assertEquals(time, updates.lastUpdated(userName).getMillis());
    }

    @Test
    public void testUpdateExistingUser()
    {
        String tableName = "testUpdateExistingUser";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, tableName);
        final long time = 3425322L;
        String userName = "Some other username";
        updates.setUpdateTime(userName, new DateTime(time));
        Assert.assertEquals(time, updates.lastUpdated(userName).getMillis());
        updates.setUpdateTime(userName, new DateTime(2 * time));
        Assert.assertEquals(2 * time, updates.lastUpdated(userName).getMillis());
    }
}
