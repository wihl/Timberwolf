package com.softartisans.timberwolf.exchange;

import java.util.ArrayList;
import java.util.List;

/** Helper class for required folders in exchange */
public class RequiredFolder
{
    private final String name;
    private String id;
    private List<RequiredFolder> folders;

    public RequiredFolder(final String folderName)
    {
        this.name = folderName;
        folders = new ArrayList<RequiredFolder>();
    }

    public String getName()
    {
        return name;
    }

    public String getId()
    {
        return id;
    }

    public void setId(final String folderId)
    {
        id = folderId;
    }

    public RequiredFolder addFolder(final String childFolder)
    {
        RequiredFolder folder = new RequiredFolder(childFolder);
        folders.add(folder);
        return folder;
    }

    public void initialize(ExchangePump pump, String user)
    {
        if (folders.size() > 0)
        {
            pump.createFolders(user, getId(), folders);
            for (RequiredFolder folder : folders)
            {
                System.err.println("    Initialized folder: " + folder.getId());
                folder.initialize(pump, user);
            }
        }
    }
}
