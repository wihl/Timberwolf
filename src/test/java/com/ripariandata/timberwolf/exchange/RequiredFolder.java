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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Helper class for required folders in exchange. */
public class RequiredFolder
{
    private static final Logger LOG = LoggerFactory.getLogger(RequiredFolder.class);
    private final String name;
    private final String emailAddress;
    private String id;
    private final List<RequiredFolder> folders;
    private final List<RequiredEmail> emails;

    public RequiredFolder(final String folderName, final String email)
    {
        this.name = folderName;
        this.emailAddress = email;
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
        RequiredFolder folder = new RequiredFolder(childFolder, emailAddress);
        folders.add(folder);
        return folder;
    }

    public RequiredEmail add(final String subject, final String body)
    {
        RequiredEmail email = new RequiredEmail(emailAddress, subject, body);
        emails.add(email);
        return email;
    }

    public void initialize(final ExchangePump pump, final String user) throws ExchangePump.FailedToCreateFolders
    {
        LOG.debug("Initializing folder: " + getName() + " with " + folders.size() + " subfolders and " + emails.size()
                  + " emails.");
        if (folders.size() > 0)
        {
            pump.createFolders(user, getId(), folders);
        }
        for (RequiredFolder folder : folders)
        {
            folder.initialize(pump, user);
        }
        for (RequiredEmail email : emails)
        {
            email.initialize(this, user);
        }
    }

    public void sendEmail(final ExchangePump pump, final String user) throws ExchangePump.FailedToCreateMessage
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
     *
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

    /** Wipes out all the emails in this folder, for getting new emails. */
    public void nextRun()
    {
        emails.clear();
        for (RequiredFolder folder : folders)
        {
            folder.nextRun();
        }
    }

    public void getAllEmails(final List<RequiredEmail> destination)
    {
        destination.addAll(emails);
        for (RequiredFolder folder : folders)
        {
            folder.getAllEmails(destination);
        }
    }

    @Override
    public String toString()
    {
        return "RequiredFolder{"
               + "name='" + name + '\''
               + ", emailAddress='" + emailAddress + '\''
               + ", id='" + id + '\''
               + ", folders=" + folders
               + ", emails=" + emails
               + '}';
    }
}
