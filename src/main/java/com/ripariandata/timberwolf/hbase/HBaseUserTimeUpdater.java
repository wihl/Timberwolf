package com.ripariandata.timberwolf.hbase;

import com.ripariandata.timberwolf.UserTimeUpdater;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.DateTime;

/**
 * An implementation of a UserTimeUpdater which stores the appropriate timings in HBase.
 */
public class HBaseUserTimeUpdater implements UserTimeUpdater
{
    /** The table wherein our timings are stored. */
    private IHBaseTable table;

    private static final String TIME_COLUMN_FAMILY = "t";
    private static final String TIME_COLUMN_QUALIFIER = "d";

    /**
     * Constructs a HBaseUserTimeUpdater from a HBaseManager and a given table name.
     * @param hBaseManager The HBaseManager to use to store our timings.
     * @param updateTable The table name to use. If the table does not exist it will be created.
     *                    Note that tables that were not created by this utility make poor storage sites.
     */
    public HBaseUserTimeUpdater(final HBaseManager hBaseManager, final String updateTable)
    {
        List<String> columnFamilies = new ArrayList<String>();
        columnFamilies.add(TIME_COLUMN_FAMILY);

        /** If the table already exists it will be simply grabbed not recreated. */
        table = hBaseManager.createTable(updateTable, columnFamilies);
    }

    /**
     * Determines when this user was last updated.
     * @param user The username.
     * @return When the user was last updated.
     */
    @Override
    public DateTime lastUpdated(final String user)
    {
        Get get = new Get(Bytes.toBytes(user));
        Result result = table.get(get);
        long timeLong = 0;
        if (!result.isEmpty())
        {
            timeLong = Bytes.toLong(result.getValue(Bytes.toBytes(TIME_COLUMN_FAMILY),
                    Bytes.toBytes(TIME_COLUMN_QUALIFIER)));
        }
        DateTime time = new DateTime(timeLong);
        return time;
    }

    /**
     * Sets the update time for a given user.
     * @param user The user who is being updated.
     * @param dateTime The datetime of the update.
     */
    @Override
    public void setUpdateTime(final String user, final DateTime dateTime)
    {
        Put put = new Put(Bytes.toBytes(user));
        put.add(Bytes.toBytes(TIME_COLUMN_FAMILY), Bytes.toBytes(TIME_COLUMN_QUALIFIER),
                Bytes.toBytes(dateTime.getMillis()));

        table.put(put);
        table.flush();
    }
}
