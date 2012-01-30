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

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.ripariandata.timberwolf.MailboxItem;
import java.util.Iterator;
import java.util.Queue;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a FindItemIterator for each folder found with findFolders.
 */
public class FindFolderIterator extends BaseChainIterator<MailboxItem>
{
    private static final Logger LOG = LoggerFactory.getLogger(FindFolderIterator.class);
    private ExchangeService service;
    private Configuration config;
    private Queue<String> folderQueue;
    private String user;
    private DateTime accessTime;

    public FindFolderIterator(final ExchangeService exchangeService, final Configuration configuration,
                              final String targetUser)
    {
        service = exchangeService;
        config = configuration;
        user = targetUser;

        try
        {
            folderQueue = FindFolderHelper.findFolders(exchangeService,
                                                       FindFolderHelper.getFindFoldersRequest(
                                                               DistinguishedFolderIdNameType.MSGFOLDERROOT), user);
            if (folderQueue.size() == 0)
            {
                LOG.warn("Did not find any folders.");
            }
        }
        catch (ServiceCallException e)
        {
            throw new ExchangeRuntimeException(
                String.format("Failed to find folder ids for %s: %s",
                              user, e.getMessage()),
                e);
        }
        catch (HttpErrorException e)
        {
            throw new ExchangeRuntimeException(
                String.format("Failed to find folder ids for %s: %s",
                              user, e.getMessage()),
                e);
        }

        accessTime = new DateTime();
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        if (folderQueue.size() == 0)
        {
            config.setLastUpdated(user, accessTime);
            return null;
        }
        FolderContext folder = new FolderContext(folderQueue.poll(), user);
        return new FindItemIterator(service, config, folder);
    }
}
