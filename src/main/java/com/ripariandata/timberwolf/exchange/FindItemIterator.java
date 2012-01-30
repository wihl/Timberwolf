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

import com.ripariandata.timberwolf.MailboxItem;
import java.util.Iterator;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a GetItemIterator over many ids, retrieved more efficiently with findItems.
 *
 * This class pages the calls to findItems.
 */
public class FindItemIterator extends BaseChainIterator<MailboxItem>
{
    private static final Logger LOG = LoggerFactory.getLogger(FindItemIterator.class);
    private ExchangeService service;
    private Configuration config;
    private FolderContext folder;
    private int currentStart;
    /**
     * If we ever make a call asking for pageSize items and get back less than that,
     * we know that there are no more messages to get, this is then set to true.
     */
    private boolean definitelyExhausted;

    public FindItemIterator(final ExchangeService exchangeService,
                            final Configuration configuration,
                            final FolderContext folderContext)
    {
        service = exchangeService;
        config = configuration;
        folder = folderContext;
        currentStart = 0;
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        try
        {
            if (definitelyExhausted)
            {
                return null;
            }
            Vector<String> messageIds = FindItemHelper.findItems(service, config, folder, currentStart);
            int pageSize = config.getFindItemPageSize();
            currentStart += pageSize;
            LOG.debug("Got {} email ids.", messageIds.size());
            if (messageIds.size() < pageSize)
            {
                definitelyExhausted = true;
            }
            if (messageIds.size() > 0)
            {
                return new GetItemIterator(service, messageIds, config, folder);
            }
            else
            {
                return null;
            }
        }
        catch (ServiceCallException e)
        {
            throw ExchangeRuntimeException.log(LOG, new ExchangeRuntimeException("Failed to find item ids.", e));
        }
        catch (HttpErrorException e)
        {
            throw ExchangeRuntimeException.log(LOG, new ExchangeRuntimeException("Failed to find item ids.", e));
        }
    }
}
