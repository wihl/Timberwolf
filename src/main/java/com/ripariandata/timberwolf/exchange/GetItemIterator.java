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
 * An iterator that calls getItem to get a list of the actual items for
 * the given list of ids.
 *
 * This class pages the calls to getItems.
 */
public class GetItemIterator extends BaseChainIterator<MailboxItem>
{

    private static final Logger LOG = LoggerFactory.getLogger(GetItemIterator.class);
    private ExchangeService service;
    private Vector<String> ids;
    private int currentStart;
    private Configuration config;
    private FolderContext folder;

    public GetItemIterator(final ExchangeService exchangeService, final Vector<String> messageIds,
                           final Configuration configuration, final FolderContext folderContext)
    {
        service = exchangeService;
        ids = messageIds;
        config = configuration;
        folder = folderContext;
        currentStart = 0;
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        if (currentStart > ids.size())
        {
            return null;
        }
        try
        {
            int pageSize = config.getGetItemPageSize();
            Vector<MailboxItem> ret = GetItemHelper.getItems(pageSize, currentStart, ids, service, folder.getUser());
            LOG.debug("Got {} email ids.", ret.size());
            currentStart += pageSize;
            return ret.iterator();
        }
        catch (ServiceCallException e)
        {
            throw ExchangeRuntimeException.log(LOG, new ExchangeRuntimeException("Failed to get emails.", e));
        }
        catch (HttpErrorException e)
        {
            throw ExchangeRuntimeException.log(LOG, new ExchangeRuntimeException("Failed to get emails.", e));
        }
    }
}
