package com.softartisans.timberwolf.hbase;

import com.softartisans.timberwolf.MockHTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.DateTime;
import org.junit.Assert;
import org.junit.Test;

public class HBaseUserTimeUpdaterTest
{

    @Test
    public void testLastUpdated()
    {
        String userTableName = "userTable";
        HBaseManager manager = new HBaseManager();
        MockHTable table = MockHTable.create(userTableName);
        IHBaseTable hbaseTable = new HBaseTable(table);
        manager.addTable(hbaseTable);

        String userName = "Robert the User";
        Put put = new Put(Bytes.toBytes(userName));
        long time = 23488902348L;
        put.add(Bytes.toBytes("t"),Bytes.toBytes("d"),Bytes.toBytes(time));
        hbaseTable.put(put);
        hbaseTable.flush();

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, userTableName);
        DateTime date = updates.LastUpdated(userName);
        Assert.assertEquals(time, date.getMillis());
    }

    @Test
    public void testLastUpdatedNoUser()
    {
        String userTableName = "userTable";
        HBaseManager manager = new HBaseManager();
        MockHTable table = MockHTable.create(userTableName);
        IHBaseTable hbaseTable = new HBaseTable(table);
        manager.addTable(hbaseTable);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, userTableName);
        DateTime date = updates.LastUpdated("not actually a username");
        Assert.assertEquals(0L, date.getMillis());
    }

    @Test
    public void testUpdateUser()
    {
        String userTableName = "userTable";
        HBaseManager manager = new HBaseManager();
        MockHTable table = MockHTable.create(userTableName);
        IHBaseTable hbaseTable = new HBaseTable(table);
        manager.addTable(hbaseTable);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, userTableName);
        long time = 23488902348L;
        String userName = "Robert the Bruce";
        updates.Updated(userName, new DateTime(time));
        Assert.assertEquals(time, updates.LastUpdated(userName).getMillis());
    }

    @Test
    public void testUpdateExistingUser()
    {
        String userTableName = "userTable";
        HBaseManager manager = new HBaseManager();
        MockHTable table = MockHTable.create(userTableName);
        IHBaseTable hbaseTable = new HBaseTable(table);
        manager.addTable(hbaseTable);

        HBaseUserTimeUpdater updates = new HBaseUserTimeUpdater(manager, userTableName);
        long time = 23488902348L;
        String userName = "Robert the Bruce";
        updates.Updated(userName, new DateTime(time));
        updates.Updated(userName, new DateTime(2*time));
        Assert.assertEquals(2*time, updates.LastUpdated(userName).getMillis());
    }
}
