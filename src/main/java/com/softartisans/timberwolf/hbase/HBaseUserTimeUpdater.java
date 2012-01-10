package com.softartisans.timberwolf.hbase;

import com.softartisans.timberwolf.UserTimeUpdater;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of a UserTimeUpdater which stores the appropriate timings in HBase.
 */
public class HBaseUserTimeUpdater implements UserTimeUpdater
{
    /** The HBaseManager for the HBase instance wherein our timings will be stored. */
    private HBaseManager manager;

    /** The table wherein our timings are stored. */
    private IHBaseTable table;

    private final String TIME_COLUMN_FAMILY = "t";
    private final String TIME_COLUMN_QUALIFIER = "d";

    /**
     * Constructs a HBaseUserTimeUpdater from a HBaseManager and a given table name.
     * @param hBaseManager The HBaseManager to use to store our timings.
     * @param updateTable
     */
    public HBaseUserTimeUpdater(final HBaseManager hBaseManager, final String updateTable)
    {
        manager = hBaseManager;

        List<String> columnFamilies = new ArrayList<String>();
        columnFamilies.add(TIME_COLUMN_FAMILY);

        /** If the table already exists it will be simply grabbed not recreated. */
        table = hBaseManager.createTable(updateTable, columnFamilies);
    }

    @Override
    public DateTime LastUpdated(String user)
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

    @Override
    public void Updated(String user, DateTime dateTime)
    {

    }
}
