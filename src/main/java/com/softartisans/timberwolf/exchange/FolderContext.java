package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseFolderIdsType;

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

    private FolderContext(final String folder, final DistinguishedFolderIdNameType.Enum distinguishedFolder,
                          final String targetUser)
    {
        stringFolder = folder;
        distinguishedFolderId = distinguishedFolder;
        user = targetUser;
    }

    public FolderContext(final String folder, final String targetUser)
    {
        this(folder, null, targetUser);
    }

    public FolderContext(final DistinguishedFolderIdNameType.Enum distinguishedFolder, final String targetUser)
    {
        this(null, distinguishedFolder, targetUser);
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
}
