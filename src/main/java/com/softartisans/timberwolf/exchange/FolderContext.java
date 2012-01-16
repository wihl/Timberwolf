package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseFolderIdsType;

/**
 * This class manages what folder an exchange
 * service should be using as a scope.
 */
public class FolderContext
{
    private final String stringFolder;
    private DistinguishedFolderIdNameType.Enum distinguishedFolderId;

    public FolderContext(final String folder)
    {
        this.stringFolder = folder;
        this.distinguishedFolderId = null;
    }

    public FolderContext(final DistinguishedFolderIdNameType.Enum distinguishedFolder)
    {
        this.stringFolder = null;
        this.distinguishedFolderId = distinguishedFolder;
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
}
