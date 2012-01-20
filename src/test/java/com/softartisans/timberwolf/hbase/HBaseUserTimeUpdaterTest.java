package com.softartisans.timberwolf.hbase;

import com.softartisans.timberwolf.MockHTable;
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
        IHBaseTable hbaseTable = new HBaseTable(table);
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
        updates.setUpdateTime(userName, new DateTime(2 * time));
        Assert.assertEquals(2 * time, updates.lastUpdated(userName).getMillis());
    }
}
