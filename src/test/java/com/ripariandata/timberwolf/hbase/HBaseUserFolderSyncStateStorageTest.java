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

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Test;

import com.ripariandata.timberwolf.MockHTable;

public class HBaseUserFolderSyncStateStorageTest
{
    private HBaseManager manager = new HBaseManager();

    private IHBaseTable mockTable(final HBaseManager hbaseManager, final String tableName)
    {
        MockHTable table = MockHTable.create(tableName);
        HBaseTable hbaseTable = new HBaseTable(table);
        hbaseManager.addTable(hbaseTable);
        return hbaseTable;
    }
    
    private Put statePut(String userName, String folderId, String state)
    {
        Put put = new Put(Bytes.toBytes(userName + " " + folderId));
        put.add(Bytes.toBytes("s"), Bytes.toBytes("v"), Bytes.toBytes(state));
        return put;
    }

    @Test
    public void testLastUpdated()
    {
        String tableName = "testSyncState";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        String userName = "Robert the User";
        String folderId = "AAFolderAA";
        final String state = "EuphoricFolderState";
        hbaseTable.put(statePut(userName, folderId, state));
        hbaseTable.flush();

        HBaseUserFolderSyncStateStorage updates = new HBaseUserFolderSyncStateStorage(manager, tableName);
        String retrievedState = updates.getLastSyncState(userName, folderId);
        Assert.assertEquals(state, retrievedState);
    }

    @Test
    public void testLastUpdatedNoUser()
    {
        String tableName = "testSyncStateNoUser";
        mockTable(manager, tableName);

        HBaseUserFolderSyncStateStorage updates = new HBaseUserFolderSyncStateStorage(manager, tableName);
        String state = updates.getLastSyncState("not actually a username", "NoId");
        Assert.assertEquals(null, state);
    }
    
    @Test
    public void testLastUpdatedNoFolderId()
    {
        String tableName = "testSyncStateNoFolderId";
        IHBaseTable hbaseTable = mockTable(manager, tableName);

        String userName = "Robert the User";
        String folderId = "ExistingAAFolderAA";
        final String state = "ExistingFolderState";
        hbaseTable.put(statePut(userName, folderId, state));
        hbaseTable.flush();
        
        HBaseUserFolderSyncStateStorage updates = new HBaseUserFolderSyncStateStorage(manager, tableName);
        String retrievedState = updates.getLastSyncState(userName, "NonExistingAAFolderAA");
        Assert.assertEquals(null, retrievedState);
    }

    @Test
    public void testUpdateUser()
    {
        String tableName = "testUpdateUser";
        mockTable(manager, tableName);

        HBaseUserFolderSyncStateStorage updates = new HBaseUserFolderSyncStateStorage(manager, tableName);
        final String state = "firstState";
        String userName = "A Generic Username";
        String folderId = "GenericFolderId";
        updates.setSyncState(userName, folderId, state);
        Assert.assertEquals(state, updates.getLastSyncState(userName, folderId));
    }

    @Test
    public void testUpdateExistingUser()
    {
        String tableName = "testUpdateExistingUser";
        mockTable(manager, tableName);

        HBaseUserFolderSyncStateStorage updates = new HBaseUserFolderSyncStateStorage(manager, tableName);
        final String state1 = "FolderState";
        final String state2 = "NextFolderState";
        String userName = "Some other username";
        String folderId = "SomeOtherFolderId";
        updates.setSyncState(userName, folderId, state1);
        Assert.assertEquals(state1, updates.getLastSyncState(userName, folderId));
        updates.setSyncState(userName, folderId, state2);
        Assert.assertEquals(state2, updates.getLastSyncState(userName, folderId));
    }
}
