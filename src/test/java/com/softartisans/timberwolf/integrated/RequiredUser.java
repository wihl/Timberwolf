package com.softartisans.timberwolf.integrated;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.softartisans.timberwolf.exchange.ExchangePump;
import com.softartisans.timberwolf.exchange.RequiredFolder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Helper class for required users in exchange */
public class RequiredUser
{
    private final String user;
    private final Map<DistinguishedFolderIdNameType.Enum, List<RequiredFolder>> distinguishedFolders;

    public RequiredUser(String username)
    {
        user = username;
        distinguishedFolders = new HashMap<DistinguishedFolderIdNameType.Enum, List<RequiredFolder>>();
    }

    public RequiredFolder addFolderToInbox(final String folderName)
    {
        return addFolder(DistinguishedFolderIdNameType.INBOX, folderName);
    }

    public RequiredFolder addFolderToRoot(final String folderName)
    {
        return addFolder(DistinguishedFolderIdNameType.MSGFOLDERROOT, folderName);
    }

    public RequiredFolder addFolder(DistinguishedFolderIdNameType.Enum parent, String folderName)
    {
        List<RequiredFolder> folders = distinguishedFolders.get(parent);
        if (folders == null)
        {
            folders = new ArrayList<RequiredFolder>();
            distinguishedFolders.put(parent, folders);
        }
        RequiredFolder folder = new RequiredFolder(folderName);
        folders.add(folder);
        return folder;
    }

    public void initialize(ExchangePump pump)
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            List<RequiredFolder> folders = distinguishedFolders.get(distinguishedFolder);
            pump.createFolders(user, distinguishedFolder, folders);
            for (RequiredFolder folder : folders)
            {
                System.err.println(" Initialized folder: " + folder.getId());
                folder.initialize(pump, user);
            }
        }
    }

    public void sendEmail(ExchangePump pump)
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            for (RequiredFolder folder : distinguishedFolders.get(distinguishedFolder))
            {
                System.err.println(" Sending email for folder: " + folder.getId());
                folder.sendEmail(pump, user);
            }
        }
    }
}
