package com.softartisans.timberwolf;

import java.util.Iterator;

/**
 * ExchangeMailStore represents a remote Exchange mail store.  It uses the
 * Exchange Web Services API to communicate with the Exchange server.
 */
public class ExchangeMailStore implements MailStore
{
    private String user;
    private String password;

    /**
     * Constructor that takes authentication information and a service endpoint.
     *
     * @param user The name of the user that ExchangeMailStore will authenticate
     * to the server with.
     *
     * @param password The password of the user that ExchangeMailStore will
     * authenticate to the server with.
     *
     * @param endpoint The location of the service endpoint that
     * ExchangeMailStore will connect to.
     */
    public ExchangeMailStore(String user, String password, String endpoint)
    {        
    }

    public Iterator<MailboxItem> getMail(String user)
    {        
        return null;
    }
}
