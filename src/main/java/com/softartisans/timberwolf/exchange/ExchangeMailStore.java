package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.MailStore;
import com.softartisans.timberwolf.MailboxItem;

import java.util.Iterator;

/**
 * This is the MailStore implementation for Exchange email.
 * This uses the Exchange web services to access exchange and get back email
 * items.
 */
public class ExchangeMailStore implements MailStore
{
    /*
     * When FindItems is run, you can limit the number of items to get at a time
     * and page, starting with 1000, but we'll probably want to profile this a
     * bit to figure out if we want more or less
     */
    private static final int MAX_FIND_ITEMS_ENTRIES = 1000;

    /**
     * GetItems takes multiple ids, but we don't want to call GetItems on all
     * MaxFindItemEntries at a time, because those could be massive responses
     * Instead, get a smaller number at a time.
     * This should evenly divide MaxFindItemEntries
     */
    private static final int MAX_GET_ITEMS_ENTRIES = 50;

    /** The service that does the sending of soap packages to exchange. */
    private final ExchangeService exchangeService;
    private final int maxFindItemsEntries;
    private final int maxGetItemsEntries;

    /**
     * Creates a new ExchangeMailStore for getting mail from the exchange
     * server at the provided url.
     *
     * @param exchangeUrl the url to the exchange web service such as
     * https://devexch01.int.tartarus.com/ews/exchange.asmx
     */
    public ExchangeMailStore(final String exchangeUrl)
    {
        this(exchangeUrl, MAX_FIND_ITEMS_ENTRIES, MAX_GET_ITEMS_ENTRIES);
    }

    /**
     * Creates an ExchangeMailStore with custom page size.
     * @param exchangeUrl the url to the exchange web service such as
     * https://devexch01.int.tartarus.com/ews/exchange.asmx.
     * @param findItemPageSize the number of ids to request at a time.
     * @param getItemPageSize the number of actual emails to request at a time.
     */
    public ExchangeMailStore(final String exchangeUrl, final int findItemPageSize, final int getItemPageSize)
    {
        this(new ExchangeService(exchangeUrl), findItemPageSize, getItemPageSize);
    }

    /**
     * Creates a new ExchangeMailStore for getting mail.
     * @param service The exchange service to use
     */
    ExchangeMailStore(final ExchangeService service)
    {
        this(service, MAX_FIND_ITEMS_ENTRIES, MAX_GET_ITEMS_ENTRIES);
    }

    ExchangeMailStore(final ExchangeService service, final int findItemPageSize, final int getItemPageSize)
    {
        exchangeService = service;
        maxFindItemsEntries = findItemPageSize;
        maxGetItemsEntries = getItemPageSize;
    }

    @Override
    public final Iterable<MailboxItem> getMail()
    {
        return new Iterable<MailboxItem>()
        {
            @Override
            public Iterator<MailboxItem> iterator()
            {
                Configuration config = new Configuration(maxFindItemsEntries, maxGetItemsEntries);
                return new FindFolderIterator(exchangeService, config);
            }
        };
    }
}
