package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseFolderIdsType;

import org.joda.time.DateTime;

/**
 * This class contains any configurable settings
 * that will effect the exchange service calls.
 */
public class Configuration
{
    private final int findPageSize;
    private final int getPageSize;
    private final String stringFolder;
    private DistinguishedFolderIdNameType.Enum distinguishedFolderId;

    public Configuration(final int findItemPageSize,
                         final int getItemPageSize,
                         final String folder)
    {
        this.findPageSize = findItemPageSize;
        this.getPageSize = getItemPageSize;
        this.stringFolder = folder;
        this.distinguishedFolderId = null;
    }

    public Configuration(final int findItemPageSize,
                         final int getItemPageSize,
                         final DistinguishedFolderIdNameType.Enum distinguishedFolder)
    {
        this.findPageSize = findItemPageSize;
        this.getPageSize = getItemPageSize;
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

    public String getFolderId()
    {
        return stringFolder;
    }

    public DateTime getStartDate()
    {
        return null;
    }

    public int getFindItemPageSize()
    {
        return findPageSize;
    }

    public int getGetItemPageSize()
    {
        return getPageSize;
    }
}
