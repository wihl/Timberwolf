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
                LOG.info("Could not find folders for {}", user);
                LOG.trace("Due to exception", e);
            }
        }
        return null;
    }

    /**
     * An iterator wrapper that wraps another iterator, and logs any exceptions
     * thrown. It then returns false on hasNext() and null on next()
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
                LOG.trace("Due to exception", e);
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
                LOG.trace("Due to exception", e);
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
