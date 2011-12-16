package com.softartisans.timberwolf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.Vector;

/**
 * This is the MailStore implementation for Exchange email.
 * It uses Exchange's built in SOAP api to get emails from the server
 */
public class ExchangeMailStore implements MailStore
{
    private static final Logger log = LoggerFactory.getLogger(
            ExchangeMailStore.class);

    /**
     * GetItems takes multiple ids, but we don't want to call GetItems on all
     * MaxFindItemEntries at a time, because those could be massive responses
     * Instead, get a smaller number at a time.
     * This should evenly divide MaxFindItemEntries
     */
    private static final int MaxGetItemsEntries = 50;

    /**
     * The url of the service, passed in as a command line parameter, or from a
     * config
     */
    private String exchangeUrl;

    public ExchangeMailStore(String exchangeUrl)
    {
        this.exchangeUrl = exchangeUrl;
    }


    @Override
    public Iterable<MailboxItem> getMail(String user)
    {
        return new Iterable<MailboxItem>()
        {
            @Override
            public Iterator<MailboxItem> iterator()
            {
                return new EmailIterator(exchangeUrl);
            }
        };
    }


    private static class EmailIterator implements Iterator<MailboxItem>
    {
        Vector<String> currentIds;
        int currentIdIndex = 0;
        int findItemsOffset = 0;
        String exchangeUrl;
        private Vector<MailboxItem> mailBoxItems;
        private int currentMailboxItemIndex = 0;


        public EmailIterator(String exchangeUrl)
        {
            this.exchangeUrl = exchangeUrl;
        }

        @Override
        public boolean hasNext()
        {
            if (currentIds == null)
            {
                currentMailboxItemIndex = 0;
                currentIdIndex = 0;
                currentIds = findItems(findItemsOffset, exchangeUrl);
                log.debug("Got " + currentIds.size() + " email ids");
            }
            // TODO paging here
            if (currentIdIndex >= currentIds.size())
            {
                return false;
            }
            if (mailBoxItems == null)
            {
                currentMailboxItemIndex = 0;
                mailBoxItems =
                        getItems(MaxGetItemsEntries, currentIdIndex, currentIds,
                                 exchangeUrl);
                log.debug("Got " + mailBoxItems.size() + " emails");
                return currentMailboxItemIndex < mailBoxItems.size();
            }
            // TODO call getItems more than once
            return currentMailboxItemIndex < mailBoxItems.size();
        }

        private static Vector<String> findItems(int findItemsOffset,
                                                String exchangeUrl)
        {
            return new Vector<String>();
        }

        private static Vector<MailboxItem> getItems(int maxGetItemsEntries,
                                                    int currentIdIndex,
                                                    Vector<String> currentIds,
                                                    String exchangeUrl)
        {
            return new Vector<MailboxItem>();
        }

        @Override
        public MailboxItem next()
        {
            return null;
        }

        @Override
        public void remove()
        {
        }
    }
}
