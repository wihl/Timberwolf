package com.softartisans.timberwolf.integrated;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.softartisans.timberwolf.exchange.ExchangePump;
import com.softartisans.timberwolf.exchange.RequiredEmail;
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
    private final Map<DistinguishedFolderIdNameType.Enum, List<RequiredEmail>> topLevelEmails;
    private static final int MAX_FIND_ITEM_ATTEMPTS = 10;

    public RequiredUser(String username)
    {
        user = username;
        distinguishedFolders = new HashMap<DistinguishedFolderIdNameType.Enum, List<RequiredFolder>>();
        topLevelEmails = new HashMap<DistinguishedFolderIdNameType.Enum, List<RequiredEmail>>();
    }

    private RequiredEmail addToFolder(final DistinguishedFolderIdNameType.Enum folder, final String toEmail,
                                      final String subjectText, final String bodyText)
    {
        List<RequiredEmail> emails = topLevelEmails.get(folder);
        if (emails == null)
        {
            emails = new ArrayList<RequiredEmail>();
            topLevelEmails.put(folder, emails);
        }
        RequiredEmail email = new RequiredEmail(toEmail, subjectText, bodyText);
        emails.add(email);
        return email;
    }

    public RequiredEmail addToInbox(final String toEmail, final String subjectText, final String bodyText)
    {
        return addToFolder(DistinguishedFolderIdNameType.INBOX, toEmail, subjectText, bodyText);
    }

    public RequiredEmail addToDeletedItems(final String toEmail, final String subjectText, final String bodyText)
    {
        return addToFolder(DistinguishedFolderIdNameType.DELETEDITEMS, toEmail, subjectText, bodyText);
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
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : topLevelEmails.keySet())
        {
            List<RequiredEmail> emails = topLevelEmails.get(distinguishedFolder);
            for (RequiredEmail email : emails)
            {
                email.initialize(distinguishedFolder);
            }
        }
    }

    public void sendEmail(ExchangePump pump) throws ExchangePump.FailedToCreateMessage
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            for (RequiredFolder folder : distinguishedFolders.get(distinguishedFolder))
            {
                System.err.println(" Sending email for folder: " + folder.getId());
                folder.sendEmail(pump, user);
            }
        }
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : topLevelEmails.keySet())
        {
            pump.sendMessages(topLevelEmails.get(distinguishedFolder));
        }
    }

    public void moveEmails(ExchangePump pump) throws ExchangePump.FailedToFindMessage, ExchangePump.FailedToMoveMessage
    {
        for (int i = 0; i < MAX_FIND_ITEM_ATTEMPTS; i++)
        {
            HashMap<String, List<ExchangePump.MessageId>> items = pump.findItems(user);
            if (checkRequiredEmails(items))
            {
                continue;
            }
            // we have succesfully gotten all the emails
            for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
            {
                for (RequiredFolder folder : distinguishedFolders.get(distinguishedFolder))
                {
                    folder.moveMessages(pump, user, items);
                }
            }

            for (DistinguishedFolderIdNameType.Enum distinguishedFolder : topLevelEmails.keySet())
            {
                if (distinguishedFolder == DistinguishedFolderIdNameType.INBOX)
                {
                    // no need to move these ones
                    continue;
                }
                pump.moveMessages(user, distinguishedFolder, items.get(distinguishedFolder.toString()));

            }
            return;
        }
    }

    private boolean checkRequiredEmails(final HashMap<String, List<ExchangePump.MessageId>> items)
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            for (RequiredFolder folder : distinguishedFolders.get(distinguishedFolder))
            {
                if (folder.checkEmailsBeforeMove(items))
                {
                    System.err.println("Ready to move emails");
                }
                else
                {
                    System.err.println("Not ready, trying again");
                    return true;
                }
            }
        }
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : topLevelEmails.keySet())
        {
            if (topLevelEmails.get(distinguishedFolder).size() != items.get(distinguishedFolder.toString()).size())
            {
                return true;
            }
        }
        return false;
    }
}
