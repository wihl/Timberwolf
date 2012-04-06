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
package com.ripariandata.timberwolf.mail.exchange;

import com.ripariandata.timberwolf.mail.MailboxItem;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a GetItemIterator over many ids, retrieved more efficiently with syncFolderItems.
 * <p/>
 * This class pages the calls to syncFolderItems.
 */
public class SyncFolderItemIterator extends BaseChainIterator<MailboxItem>
{
    private static final Logger LOG = LoggerFactory.getLogger(SyncFolderItemIterator.class);
    private ExchangeService service;
    private Configuration config;
    private FolderContext folder;
    private boolean retrievedLastItem;
    private String syncState;

    public SyncFolderItemIterator(final ExchangeService exchangeService,
                                  final Configuration configuration,
                                  final FolderContext folderContext)
    {
        service = exchangeService;
        config = configuration;
        folder = folderContext;
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        try
        {
            if (syncState == null)
            {
                // if this is the first run, just get the stored sync state
                syncState = folder.getSyncStateToken();
            }
            else
            {
                // We have successfully retrieved all the items for the given sync state,
                // so we can now store that sync state in the folder context.
                folder.setSyncStateToken(syncState);
            }
            if (retrievedLastItem)
            {
                return null;
            }
            SyncFolderItemsHelper.SyncFolderItemsResult result =
                    SyncFolderItemsHelper.syncFolderItems(service, config, folder);
            syncState = result.getSyncState();
            LOG.debug("Got {} email ids, which were {}the last of them.", result.getIds().size(),
                      result.includesLastItem() ? "" : "not ");
            retrievedLastItem = result.includesLastItem();
            if (result.getIds().size() > 0)
            {
                return new GetItemIterator(service, result.getIds(), config, folder);
            }
            else
            {
                return null;
            }
        }
        catch (ServiceCallException e)
        {
            throw ExchangeRuntimeException.log(LOG, new ExchangeRuntimeException("Failed to sync folder items.", e));
        }
        catch (HttpErrorException e)
        {
            throw ExchangeRuntimeException.log(LOG, new ExchangeRuntimeException("Failed to sync folder items.", e));
        }
    }
}
