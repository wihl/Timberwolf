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

import com.ripariandata.timberwolf.UserFolderSyncStateStorage;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

/** Maintains the userFolder sync state on hbase tables. */
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
    public String getLastSyncState(final String user, final String folderId)
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
    public void setSyncState(final String user,
                             final String folderId,
                             final String syncState)
    {
        Put put = new Put(primaryKey(user, folderId));
        put.add(Bytes.toBytes(SYNC_COLUMN_FAMILY), Bytes.toBytes(SYNC_COLUMN_QUALIFIER),
                Bytes.toBytes(syncState));

        table.put(put);
        table.flush();
    }

    /** Given the user and folderId of interest, return the expected key. */
    private byte[] primaryKey(final String user, final String folderId)
    {
        return Bytes.toBytes(user + " " + folderId);
    }
}
