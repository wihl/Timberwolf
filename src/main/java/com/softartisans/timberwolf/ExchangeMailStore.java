package com.softartisans.timberwolf;

import java.util.Iterator;

/**
 * ExchangeMailStore represents a remote Exchange mail store.  It uses the
 * Exchange Web Services API to communicate with the Exchange server.
 */
public class ExchangeMailStore implements MailStore
{
    public Iterator<MailboxItem> getMail(String user)
    {
        throw new UnsupportedOperationException(
            "This method is not implemented yet.");
    }
}
