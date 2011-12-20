package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;

/** 
 * An http connection factory that returns mock connections.  These can be used
 * for testing.
 */
public class MockHttpUrlConnectionFactory implements HttpUrlConnectionFactory
{
    public HttpURLConnection newInstance(String address, byte[] request)
        throws MalformedURLException, IOException, ProtocolException,
               AuthenticationException
    {
        return null;
    }
}
