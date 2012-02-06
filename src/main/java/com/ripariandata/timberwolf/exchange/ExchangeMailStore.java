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

import com.ripariandata.timberwolf.MailStore;
import com.ripariandata.timberwolf.MailboxItem;
import com.ripariandata.timberwolf.UserFolderSyncStateStorage;

import java.util.Iterator;

/**
 * This is the MailStore implementation for Exchange email.
 * This uses the Exchange web services to access exchange and get back email
 * items.
 */
public class ExchangeMailStore implements MailStore
{
    /*
     * When you get the ids for messages you can control the number of items
     * returned at a time,
     */
    private static final int DEFAULT_ID_PAGE_SIZE = SyncFolderItemsHelper.MAX_SYNC_COUNT;

    /**
     * GetItems takes multiple ids, but we don't want to call GetItems on all
     * DEFAULT_ID_PAGE_SIZE at a time, because those could be massive responses
     * Instead, get a smaller number at a time.
     * This should evenly divide DEFAULT_ID_PAGE_SIZE.
     *
     */
    private static final int DEFAULT_ITEM_PAGE_SIZE = 64;

    /** The service that does the sending of soap packages to exchange. */
    private final ExchangeService exchangeService;
    private Configuration config;

    /**
     * Creates a new ExchangeMailStore for getting mail from the exchange
     * server at the provided url.
     *
     * @param exchangeUrl the url to the exchange web service such as
     * https://devexch01.int.tartarus.com/ews/exchange.asmx
     */
    public ExchangeMailStore(final String exchangeUrl)
    {
        this(exchangeUrl, DEFAULT_ID_PAGE_SIZE, DEFAULT_ITEM_PAGE_SIZE);
    }

    /**
     * Creates an ExchangeMailStore with custom page size.
     *
     * @param exchangeUrl the url to the exchange web service such as
     * https://devexch01.int.tartarus.com/ews/exchange.asmx.
     * @param idPagesSize the number of ids to request at a time.
     * @param itemPageSize the number of actual emails to request at a time.
     */
    public ExchangeMailStore(final String exchangeUrl, final int idPagesSize, final int itemPageSize)
    {
        this(new ExchangeService(exchangeUrl), idPagesSize, itemPageSize);
    }

    /**
     * Creates a new ExchangeMailStore for getting mail.
     *
     * @param service The exchange service to use
     */
    ExchangeMailStore(final ExchangeService service)
    {
        this(service, DEFAULT_ID_PAGE_SIZE, DEFAULT_ITEM_PAGE_SIZE);
    }

    ExchangeMailStore(final ExchangeService service, final int idPageSize, final int itemPageSize)
    {
        exchangeService = service;
        config = new Configuration(idPageSize, itemPageSize);
    }

    @Override
    public final Iterable<MailboxItem> getMail(final Iterable<String> users,
                                               final UserFolderSyncStateStorage syncStateStorage)
    {
        return new Iterable<MailboxItem>()
        {
            @Override
            public Iterator<MailboxItem> iterator()
            {
                return new UserIterator(exchangeService, config.withSyncStateStorage(syncStateStorage), users);
            }
        };
    }
}
