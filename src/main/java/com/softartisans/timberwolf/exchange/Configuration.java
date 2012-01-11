package com.softartisans.timberwolf.exchange;

import org.joda.time.DateTime;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseFolderIdsType;

public class Configuration {
    private final int findItemPageSize;
    private final int getItemPageSize;
    private final String folder;
    DistinguishedFolderIdNameType.Enum distinguishedFolder;

    public Configuration(int findItemPageSize,
                         int getItemPageSize,
                         String folder)
    {
        this.findItemPageSize = findItemPageSize;
        this.getItemPageSize = getItemPageSize;
        this.folder = folder;
        this.distinguishedFolder = null;
    }

    public Configuration(int findItemPageSize, int getItemPageSize,
                         DistinguishedFolderIdNameType.Enum distinguishedFolder)
    {
        this.findItemPageSize = findItemPageSize;
        this.getItemPageSize = getItemPageSize;
        this.folder = null;
        this.distinguishedFolder = distinguishedFolder;
    }

    public NonEmptyArrayOfBaseFolderIdsType getFolderIds()
    {
        NonEmptyArrayOfBaseFolderIdsType ids =
                NonEmptyArrayOfBaseFolderIdsType.Factory.newInstance();
        if (folder != null)
        {
            FolderIdType folderType = ids.addNewFolderId();
            folderType.setId(folder);
        }
        else
        {
            DistinguishedFolderIdType folderId =
                    ids.addNewDistinguishedFolderId();
            folderId.setId(distinguishedFolder);
        }
        return ids;
    }

    public String getFolderId()
    {
        return folder;
    }

    public DateTime getStartDate()
    {
        return null;
    }

    public int getFindItemPageSize()
    {
        return findItemPageSize;
    }

    public int getGetItemPageSize()
    {
        return getItemPageSize;
    }
}
