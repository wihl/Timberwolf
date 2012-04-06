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
package com.ripariandata.timberwolf.mail.exchange;

import com.ripariandata.timberwolf.writer.UserFolderSyncStateStorage;
import com.ripariandata.timberwolf.writer.console.InMemoryUserFolderSyncStateStorage;

/**
 * This class contains any configurable settings
 * that will effect the exchange service calls.
 */
public class Configuration
{
    private final int idPageSize;
    private final int itemPageSize;
    private UserFolderSyncStateStorage syncStateStorage;

    /**
     * @param sizeOfIdPages Must be greater than or equal to 1.
     * @param sizeOfItemPages Must be greater than or equal to 1.
     * @param userFolderSyncStateStorage The storage that maintains sync states
     * for all the folders for all the users.
     */
    public Configuration(final int sizeOfIdPages, final int sizeOfItemPages,
                         final UserFolderSyncStateStorage userFolderSyncStateStorage)
    {
        // Asking for negative or zero max items is nonsensical.
        idPageSize = Math.max(sizeOfIdPages, 1);
        itemPageSize = Math.max(sizeOfItemPages, 1);
        syncStateStorage = userFolderSyncStateStorage;
    }

    /**
     * Creates a configuration with the startDate set to the beginning of the epoch.
     *
     * @param sizeOfIdPages Must be greater than or equal to 1.
     * @param sizeOfItemPages Must be greater than or equal to 1.
     */
    public Configuration(final int sizeOfIdPages, final int sizeOfItemPages)
    {
        this(sizeOfIdPages, sizeOfItemPages, new InMemoryUserFolderSyncStateStorage());
    }

    public int getIdPageSize()
    {
        return idPageSize;
    }

    public int getItemPageSize()
    {
        return itemPageSize;
    }

    public UserFolderSyncStateStorage getSyncStateStorage()
    {
        return syncStateStorage;
    }

    public Configuration withSyncStateStorage(final UserFolderSyncStateStorage userFolderSyncStateStorage)
    {
        return new Configuration(idPageSize, itemPageSize, userFolderSyncStateStorage);
    }
}
