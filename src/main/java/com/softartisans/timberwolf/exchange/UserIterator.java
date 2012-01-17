package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.MailboxItem;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** An iterator that runs FindFolderIterators for many users, flattening the results. */
public class UserIterator extends BaseChainIterator<MailboxItem>
{
    private static final Logger LOG = LoggerFactory.getLogger(UserIterator.class);
    private ExchangeService service;
    private Iterator<String> users;
    private Configuration config;

    public UserIterator(final ExchangeService exchangeService, final Configuration configuration,
                        final Iterable<String> targetUsers)
    {
        service = exchangeService;
        config = configuration;
        users = targetUsers.iterator();
    }

    @Override
    protected Iterator<MailboxItem> createIterator()
    {
        while (users.hasNext())
        {
            String user = users.next();
            try
            {
                return new SafeIterator(user, new FindFolderIterator(service, config, user));
            }
            catch (Exception e)
            {
                LOG.warn("Failed to find folders for user: {}", user);
                LOG.debug("Due to exception", e);
            }
        }
        return null;
    }

    /**
     * An iterator wrapper that wraps another iterator, and logs any exceptions
     * thrown. I then returns false on hasNext() and null on next()
     */
    private static class SafeIterator implements Iterator<MailboxItem>
    {

        private String user;
        private Iterator<MailboxItem> baseIterator;

        public SafeIterator(final String currentUser, final Iterator<MailboxItem> iterator)
        {
            user = currentUser;
            baseIterator = iterator;
        }

        @Override
        public boolean hasNext()
        {
            try
            {
                return baseIterator.hasNext();
            }
            catch (Exception e)
            {
                LOG.warn("Failed to get email for user: {}", user);
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Due to exception", e);
                }

            }
            return false;
        }

        @Override
        public MailboxItem next()
        {
            try
            {
                return baseIterator.next();
            }
            catch (Exception e)
            {
                LOG.warn("Failed to get email for user: {}", user);
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Due to exception", e);
                }
            }
            return null;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }

}
