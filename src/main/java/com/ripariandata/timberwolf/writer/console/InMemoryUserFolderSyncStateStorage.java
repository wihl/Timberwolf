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
package com.ripariandata.timberwolf.writer.console;

import com.ripariandata.timberwolf.writer.UserFolderSyncStateStorage;

import java.util.HashMap;
import java.util.Map;

/**
 * A basic implementation of UserFolderSyncStorage that just stores the states
 * in memory.
 */
public class InMemoryUserFolderSyncStateStorage implements UserFolderSyncStateStorage
{
    private Map<String, Map<String, String>> syncStates;

    public InMemoryUserFolderSyncStateStorage()
    {
        syncStates = new HashMap<String, Map<String, String>>();
    }

    @Override
    public String getLastSyncState(final String user, final String folderId)
    {
        Map<String, String> folders = syncStates.get(user);
        if (folders == null)
        {
            return null;
        }
        return folders.get(folderId);
    }

    @Override
    public void setSyncState(final String user, final String folderId, final String syncState)
    {
        Map<String, String> folders = syncStates.get(user);
        if (folders == null)
        {
            folders = new HashMap<String, String>();
            syncStates.put(user, folders);
        }
        folders.put(folderId, syncState);

    }
}
