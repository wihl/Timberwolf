package com.softartisans.timberwolf.exchange;

import java.net.HttpURLConnection;

/** Represents a factory for HTTP requests. */
interface HttpUrlConnectionFactory
{
    /**
     * Constructs a new HttpURLConnection that will make a request to the
     * specified url with the specified request as a payload.
     */
    HttpURLConnection newInstance(String address, byte[] request)
        throws com.softartisans.timberwolf.exchange.HttpUrlConnectionCreationException;
}
