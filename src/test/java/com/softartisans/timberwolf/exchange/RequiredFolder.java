package com.softartisans.timberwolf.exchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Helper class for required folders in exchange */
public class RequiredFolder
{
    private final String name;
    private String id;
    private List<RequiredFolder> folders;
    private List<RequiredEmail> emails;

    public RequiredFolder(final String folderName)
    {
        this.name = folderName;
        folders = new ArrayList<RequiredFolder>();
        emails = new ArrayList<RequiredEmail>();
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

    public RequiredEmail add(String to, String subject, String body)
    {
        RequiredEmail email = new RequiredEmail(to, subject, body);
        emails.add(email);
        return email;
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
        for (RequiredEmail email : emails)
        {
            email.initialize(this, user);
        }
    }

    public void sendEmail(ExchangePump pump, String user) throws ExchangePump.FailedToCreateMessage
    {
        if (emails.size() > 0)
        {
            pump.sendMessages(emails);
        }
        if (folders.size() > 0)
        {
            for (RequiredFolder folder : folders)
            {
                folder.sendEmail(pump, user);
            }
        }

    }

    /**
     * This confirms that the number of message ids in 'items' corresponds to
     * the number of messages this folder contains. This recursively performs
     * the same operation in all child folders as well.

     * @return true if all emails match up correctly, false otherwise
     */
    public boolean checkEmailsBeforeMove(final HashMap<String, List<ExchangePump.MessageId>> items)
    {
        List<ExchangePump.MessageId> messageIds = items.get(id);
        int idSize = messageIds == null ? 0 : messageIds.size();
        if (emails.size() != idSize)
        {
            return false;
        }
        for (RequiredFolder folder : folders)
        {
            if (!folder.checkEmailsBeforeMove(items))
            {
                return false;
            }
        }
        return true;
    }

    public void moveMessages(final ExchangePump pump, final String user,
                             final HashMap<String, List<ExchangePump.MessageId>> items)
            throws ExchangePump.FailedToMoveMessage
    {
        if (emails.size() > 0)
        {
            pump.moveMessages(user, id, items.get(id));
        }
        for (RequiredFolder folder : folders)
        {
            folder.moveMessages(pump, user, items);
        }
    }

    public void getAllEmails(List<RequiredEmail> destination)
    {
        destination.addAll(emails);
        for (RequiredFolder folder : folders)
        {
            folder.getAllEmails(destination);
        }
    }
}
