package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;

/** Represents a factory for HTTP requests. */
interface HttpUrlConnectionFactory
{
    /** 
     * Constructs a new HttpURLConnection that will make a request to the
     * specified url with the specified request as a payload.
     */
    public HttpURLConnection newInstance(String address, byte[] request)
        throws MalformedURLException, IOException, ProtocolException,
               AuthenticationException;
}
