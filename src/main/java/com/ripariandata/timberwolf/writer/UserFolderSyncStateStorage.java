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
package com.ripariandata.timberwolf.writer;

/**
 * Stores all the sync states for all the folders for all the users,
 * potentially in a database or on disk.
 */
public interface UserFolderSyncStateStorage
{
    /**
     * Returns the SyncState token for the last time the given folder
     * was synced for the given user.
     * @param user The user to check against.
     * @param folderId One of user's folders.
     * @return The last sync state that was stored for the given user's
     * given folder.
     */
    String getLastSyncState(String user, String folderId);

    /**
     * Set the sync state token for the given folder in the given user's
     * mailbox.
     * @param user The user who's mailbox was synced.
     * @param folderId The folder in said inbox.
     * @param syncState The sync state token.
     */
    void setSyncState(String user, String folderId, String syncState);
}
