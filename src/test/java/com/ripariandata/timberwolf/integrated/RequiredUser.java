package com.ripariandata.timberwolf.integrated;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.ripariandata.timberwolf.exchange.ExchangePump;
import com.ripariandata.timberwolf.exchange.ExchangePump.FailedToDeleteMessage;
import com.ripariandata.timberwolf.exchange.ExchangePump.FailedToFindMessage;
import com.ripariandata.timberwolf.exchange.ExchangePump.MessageId;
import com.ripariandata.timberwolf.exchange.RequiredEmail;
import com.ripariandata.timberwolf.exchange.RequiredFolder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for required users in exchange. */
public class RequiredUser
{

    private static final Logger LOG = LoggerFactory.getLogger(RequiredUser.class);
    private final String user;
    private final Map<DistinguishedFolderIdNameType.Enum, List<RequiredFolder>> distinguishedFolders;
    private final Map<DistinguishedFolderIdNameType.Enum, List<RequiredEmail>> topLevelEmails;
    private final List<RequiredEmail> drafts;
    private final List<RequiredEmail> sentItems;
    private static final int MAX_FIND_ITEM_ATTEMPTS = 10;
    private final String emailAddress;

    public RequiredUser(final String username, final String domain)
    {
        user = username;
        emailAddress = username + "@" + domain;
        distinguishedFolders = new HashMap<DistinguishedFolderIdNameType.Enum, List<RequiredFolder>>();
        topLevelEmails = new HashMap<DistinguishedFolderIdNameType.Enum, List<RequiredEmail>>();
        drafts = new ArrayList<RequiredEmail>();
        sentItems = new ArrayList<RequiredEmail>();
    }

    private RequiredEmail addToFolder(final DistinguishedFolderIdNameType.Enum folder,
                                      final String subjectText, final String bodyText)
    {
        List<RequiredEmail> emails = topLevelEmails.get(folder);
        if (emails == null)
        {
            emails = new ArrayList<RequiredEmail>();
            topLevelEmails.put(folder, emails);
        }
        RequiredEmail email = new RequiredEmail(emailAddress, subjectText, bodyText);
        emails.add(email);
        return email;
    }

    public RequiredEmail addToInbox(final String subjectText, final String bodyText)
    {
        return addToFolder(DistinguishedFolderIdNameType.INBOX, subjectText, bodyText);
    }

    public RequiredEmail addToDeletedItems(final String subjectText, final String bodyText)
    {
        return addToFolder(DistinguishedFolderIdNameType.DELETEDITEMS, subjectText, bodyText);
    }

    public RequiredEmail addDraft(final String toEmail, final String subject, final String body)
    {
        RequiredEmail email = new RequiredEmail(toEmail, subject, body);
        drafts.add(email);
        return email;
    }

    public RequiredEmail addSentItem(final String toEmail, final String subject, final String body)
    {
        RequiredEmail email = new RequiredEmail(toEmail, subject, body).from(emailAddress);
        sentItems.add(email);
        return email;
    }

    public RequiredFolder addFolderToInbox(final String folderName)
    {
        return addFolder(DistinguishedFolderIdNameType.INBOX, folderName);
    }

    public RequiredFolder addFolderToRoot(final String folderName)
    {
        return addFolder(DistinguishedFolderIdNameType.MSGFOLDERROOT, folderName);
    }

    public RequiredFolder addFolder(final DistinguishedFolderIdNameType.Enum parent, final String folderName)
    {
        List<RequiredFolder> folders = distinguishedFolders.get(parent);
        if (folders == null)
        {
            folders = new ArrayList<RequiredFolder>();
            distinguishedFolders.put(parent, folders);
        }
        RequiredFolder folder = new RequiredFolder(folderName, emailAddress);
        folders.add(folder);
        return folder;
    }

    /**
     * All RequiredEmails will have their folderId set to their containing
     * folder. All folders are created on exchange and their folderId is set
     * the the resulting folderId Exchange assigned.
     *
     * @param pump The ExchangePump used to manage exchange.
     */
    public void initialize(final ExchangePump pump)
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            List<RequiredFolder> folders = distinguishedFolders.get(distinguishedFolder);
            pump.createFolders(user, distinguishedFolder, folders);
            for (RequiredFolder folder : folders)
            {
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
        for (RequiredEmail email : drafts)
        {
            email.initialize(DistinguishedFolderIdNameType.DRAFTS);
        }
        for (RequiredEmail email : sentItems)
        {
            email.initialize(DistinguishedFolderIdNameType.SENTITEMS);
        }
    }

    public void sendEmail(final ExchangePump pump) throws ExchangePump.FailedToCreateMessage
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            for (RequiredFolder folder : distinguishedFolders.get(distinguishedFolder))
            {
                LOG.debug(" Sending email for folder: " + folder.getId());
                folder.sendEmail(pump, user);
            }
        }
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : topLevelEmails.keySet())
        {
            pump.sendMessages(topLevelEmails.get(distinguishedFolder));
        }
        if (drafts.size() > 0)
        {
            pump.saveDrafts(user, drafts);
        }
        if (sentItems.size() > 0)
        {
            pump.sendAndSave(user, sentItems);
        }
    }

    /**
     * This deletes all created RequiredEmails and RequiredFolder. It may
     * also delete some emails which were already in exchange. But they
     * shouldn't be there anyway.
     *
     * @param pump The pump used to manage Exchange
     */
    public void deleteEmails(final ExchangePump pump) throws FailedToDeleteMessage, FailedToFindMessage
    {
        // First we start out by deleting all instantiated folders. This'll
        // delete anything that exists within those folders.
        ArrayList<RequiredFolder> allFolders = new ArrayList<RequiredFolder>();
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            allFolders.addAll(distinguishedFolders.get(distinguishedFolder));
        }
        pump.deleteFolders(user, allFolders);

        ArrayList<MessageId> allItems = new ArrayList<MessageId>();
        // This is a list of all folders we want to clear.
        DistinguishedFolderIdNameType.Enum[] foldersToClear = {
                DistinguishedFolderIdNameType.INBOX,
                DistinguishedFolderIdNameType.DRAFTS,
                DistinguishedFolderIdNameType.SENTITEMS,
                DistinguishedFolderIdNameType.DELETEDITEMS,
        };

        // We go through each folder in the list, query exchange for all items
        // in that list, and then add them to the accumulative allItems.
        for (DistinguishedFolderIdNameType.Enum currentFolder : foldersToClear)
        {
            HashMap<String, List<MessageId>> items = pump.findItems(user, currentFolder);
            for (String subFolder : items.keySet())
            {
                allItems.addAll(items.get(subFolder));
            }
        }
        // Now we tell exchange to delete all accumulated items, all in one go.
        pump.deleteEmails(user, allItems);
    }

    public void moveEmails(final ExchangePump pump)
            throws ExchangePump.FailedToFindMessage, ExchangePump.FailedToMoveMessage
    {
        for (int i = 0; i < MAX_FIND_ITEM_ATTEMPTS; i++)
        {
            HashMap<String, List<ExchangePump.MessageId>> items =
                    pump.findItems(user, DistinguishedFolderIdNameType.INBOX);
            if (!checkRequiredEmails(items))
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
            // no need to move the drafts or sentItems lists, those are saved directly
            return;
        }
    }

    /**
     * Starts another run, so that more emails can be sent, or folders created
     * without recreating/resending the original emails/folders. This wipes out
     * all emails retrieved from getAllEmails, only the new ones created after
     * calling this will be returned.
     */
    public void nextRun()
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            List<RequiredFolder> folders = distinguishedFolders.get(distinguishedFolder);
            for (RequiredFolder folder : folders)
            {
                folder.nextRun();
            }
        }
        topLevelEmails.clear();
        drafts.clear();
        sentItems.clear();
    }

    /**
     * This ensure that all the message ids in items corresponds to the
     * messages for this user.
     *
     * @return false if message check up correctly, true otherwise
     */
    private boolean checkRequiredEmails(final HashMap<String, List<ExchangePump.MessageId>> items)
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            for (RequiredFolder folder : distinguishedFolders.get(distinguishedFolder))
            {
                if (!folder.checkEmailsBeforeMove(items))
                {
                    return false;
                }
            }
        }
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : topLevelEmails.keySet())
        {
            List<ExchangePump.MessageId> emails = items.get(distinguishedFolder.toString());
            int itemSize = emails == null ? 0 : emails.size();
            if (topLevelEmails.get(distinguishedFolder).size() != itemSize)
            {
                return false;
            }
        }
        return true;
        // no need to check the drafts or sentItems lists, those are saved directly
    }

    /**
     * This populates 'destination' with all emails of the user by recursively
     * walking through all the folders.
     */
    public void getAllEmails(final List<RequiredEmail> destination)
    {
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : distinguishedFolders.keySet())
        {
            for (RequiredFolder folder : distinguishedFolders.get(distinguishedFolder))
            {
                folder.getAllEmails(destination);
            }
        }
        for (DistinguishedFolderIdNameType.Enum distinguishedFolder : topLevelEmails.keySet())
        {
            destination.addAll(topLevelEmails.get(distinguishedFolder));
        }
        destination.addAll(drafts);
        destination.addAll(sentItems);
    }
}
