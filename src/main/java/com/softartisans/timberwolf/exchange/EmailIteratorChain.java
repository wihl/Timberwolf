package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.MailboxItem;

import java.util.Iterator;

/**
 * EmailIteratorChain connects many EmailIterators together as a single iterator,
 * crossing multiple mailboxes.
 */
public class EmailIteratorChain implements Iterator<MailboxItem>
{
    private ExchangeService service;
    private Iterator<String> users;
    private ExchangeMailStore.EmailIterator currentIterator;

    public EmailIteratorChain(final ExchangeService exchangeService, final Iterable<String> targetUsers)
    {
        service = exchangeService;
        users = targetUsers.iterator();

        currentIterator = nextViableIterator();
    }

    private ExchangeMailStore.EmailIterator nextViableIterator()
    {
        while (users.hasNext())
        {
            ExchangeMailStore.EmailIterator itor = new ExchangeMailStore.EmailIterator(service, users.next());
            if (itor.hasNext())
            {
                return itor;
            }
        }

        return null;
    }

    public boolean hasNext()
    {
        if (currentIterator != null && currentIterator.hasNext())
        {
            return true;
        }

        currentIterator = nextViableIterator();
        return currentIterator != null && currentIterator.hasNext();
    }

    public MailboxItem next()
    {
        if (!hasNext())
        {
            return null;
        }

        // By short-circuiting on hasNext(), we know that this condition implies users.hasNext().
        if (currentIterator == null || !currentIterator.hasNext())
        {
            currentIterator = nextViableIterator();
        }

        if (currentIterator != null)
        {
            return currentIterator.next();
        }

        return null;
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
