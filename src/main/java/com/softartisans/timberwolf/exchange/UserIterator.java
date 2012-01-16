package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.MailboxItem;
import java.util.Iterator;

/**
 * An iterator that runs FindFolderIterators for many users, flattening the results.
 */
public class UserIterator extends BaseChainIterator<MailboxItem>
{
    private ExchangeService service;
    private Iterator<String> users;
    private Configuration config;

    public UserIterator(final ExchangeService exchangeService, Configuration configuration,
                        final Iterable<String> targetUsers)
    {
        service = exchangeService;
        config = configuration;
        users = targetUsers.iterator();
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        if (!users.hasNext())
        {
            return null;
        }

        return new FindFolderIterator(service, config, users.next());
    }
}
