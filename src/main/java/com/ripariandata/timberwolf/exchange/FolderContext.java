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
package com.ripariandata.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.schemas.exchange.services.x2006.types.TargetFolderIdType;

/**
 * FolderContext class holds information about where a service call should be looking for
 * emails.  The context is defined by a user and folder.  Folders can be named either
 * with a DistinguishedFolderId (for standard folders like Inbox and Sent Items), or
 * by a folder ID String (for folders discovered by other service calls).  Users are
 * identified with their principal name as a String.
 */
public class FolderContext
{
    private final String stringFolder;
    private DistinguishedFolderIdNameType.Enum distinguishedFolderId;
    private final String user;
    private String syncStateToken;

    private FolderContext(final String folder, final DistinguishedFolderIdNameType.Enum distinguishedFolder,
                          final String targetUser, final String syncState)
    {
        stringFolder = folder;
        distinguishedFolderId = distinguishedFolder;
        user = targetUser;
        syncStateToken = syncState;
    }

    public FolderContext(final String folder, final String targetUser)
    {
        this(folder, null, targetUser, null);
    }

    public FolderContext(final DistinguishedFolderIdNameType.Enum distinguishedFolder, final String targetUser)
    {
        this(null, distinguishedFolder, targetUser, null);
    }

    public FolderContext(final String folder, final String targetUser, final String syncState)
    {
        this(folder, null, targetUser, syncState);
    }

    public FolderContext(final DistinguishedFolderIdNameType.Enum distinguishedFolder, final String targetUser,
                         final String syncState)
    {
        this(null, distinguishedFolder, targetUser, syncState);
    }


    public NonEmptyArrayOfBaseFolderIdsType getFolderIds()
    {
        NonEmptyArrayOfBaseFolderIdsType ids =
                NonEmptyArrayOfBaseFolderIdsType.Factory.newInstance();
        if (stringFolder != null)
        {
            FolderIdType folderType = ids.addNewFolderId();
            folderType.setId(stringFolder);
        }
        else
        {
            DistinguishedFolderIdType folderId =
                    ids.addNewDistinguishedFolderId();
            folderId.setId(distinguishedFolderId);
        }
        return ids;
    }

    public String getUser()
    {
        return user;
    }

    /**
     * Returns the sync token that should be used when syncing this folder.
     * This variable is cached, and only requires a call to the data store
     * the first time it is accessed, if that.
     * @return The sync token, or the empty string if there is no token.
     */
    public String getSyncStateToken()
    {
        return syncStateToken == null ? "" : syncStateToken;
    }

    /**
     * Sets the sync token returned from Exchange when syncing this folder.
     * This should not be called until after all items are retrieved; this can
     * be considered a permanent change; the old sync state is gone forever.
     * @param syncState The sync token from exchange. This should not be null.
     */
    public void setSyncStateToken(final String syncState)
    {
        syncStateToken = syncState;
    }

    /**
     * Returns the target folder to be sent to exchange for this folder
     * context.
     * @return A TargetFolderIdType containing this folder context.
     */
    public TargetFolderIdType getTargetFolder()
    {
        TargetFolderIdType targetFolderId = TargetFolderIdType.Factory.newInstance();
        if (stringFolder != null)
        {
            targetFolderId.addNewFolderId().setId(stringFolder);
        }
        else
        {
            targetFolderId.addNewDistinguishedFolderId().setId(distinguishedFolderId);
        }
        return targetFolderId;
    }
}
