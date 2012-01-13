package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.MailboxItem;
import java.util.Iterator;

/**
 * An iterator that runs FindFolderIterators for many users, flattening the results.
 */
public class UserIterator extends BaseChainIterator<MailboxItem>
{
    private ExchangeService service;
    private int findItemPageSize;
    private int getItemPageSize;
    private Iterator<String> users;

    public UserIterator(final ExchangeService exchangeService, final int idsPageSize, final int itemsPageSize,
                        Iterable<String> targetUsers)
    {
        service = exchangeService;
        findItemPageSize = idsPageSize;
        getItemPageSize = itemsPageSize;
        users = targetUsers.iterator();
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        if (!users.hasNext())
        {
            return null;
        }

        return new FindFolderIterator(service, findItemPageSize, getItemPageSize, users.next());
    }
}
