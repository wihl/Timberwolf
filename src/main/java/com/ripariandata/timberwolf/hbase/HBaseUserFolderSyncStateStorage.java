package com.ripariandata.timberwolf.hbase;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import com.ripariandata.timberwolf.UserFolderSyncStateStorage;

public class HBaseUserFolderSyncStateStorage implements UserFolderSyncStateStorage
{
    /** The table wherein our sync states are stored. */
    private IHBaseTable table;
    
    private static final String SYNC_COLUMN_FAMILY = "s";
    private static final String SYNC_COLUMN_QUALIFIER = "v";
    
    /**
     * Constructs a HBaseUserFolderSyncStateStorage
     * from a HBaseManager and a given table name.
     * @param hBaseManager The HBaseManager to use to store our sync states.
     * @param updateTable The table name to use. If the table does not exist
     *                    it will be created. Note that tables that were not
     *                    created by this utility make poor storage sites.
     */
    public HBaseUserFolderSyncStateStorage(final HBaseManager hBaseManager, final String updateTable)
    {
        List<String> columnFamilies = new ArrayList<String>();
        columnFamilies.add(SYNC_COLUMN_FAMILY);

        /** If the table already exists it will be simply grabbed not recreated. */
        table = hBaseManager.createTable(updateTable, columnFamilies);
    }
    
    /**
     * Determines the last sync state of this user for the given folder.
     * @param user The username.
     * @param folderId The current folder of the username.
     * @return The last recorded sync state for that folder.
     */
    @Override
    public String getLastSyncState(String user, String folderId)
    {
        Get get = new Get(primaryKey(user, folderId));
        Result result = table.get(get);
        String stateString = null;
        if (!result.isEmpty())
        {
            stateString = Bytes.toString(result.getValue(Bytes.toBytes(SYNC_COLUMN_FAMILY),
                                                         Bytes.toBytes(SYNC_COLUMN_QUALIFIER)));
        }
        return stateString;
    }

    /**
     * Sets the sync state for a folder of a given user.
     * @param user The user who is being updated.
     * @param folderId The current folder of the user that is being updated.
     * @param syncState The new folder state to record for later.
     */
    @Override
    public void setSyncState(String user, String folderId, String syncState)
    {
        Put put = new Put(primaryKey(user, folderId));
        put.add(Bytes.toBytes(SYNC_COLUMN_FAMILY), Bytes.toBytes(SYNC_COLUMN_QUALIFIER),
                Bytes.toBytes(syncState));

        table.put(put);
        table.flush();
    }
    
    /** Given the user and the folderId of interest, return the expected key */
    private byte[] primaryKey(String user, String folderId)
    {
        return Bytes.toBytes(user + " " + folderId);
    }
}
