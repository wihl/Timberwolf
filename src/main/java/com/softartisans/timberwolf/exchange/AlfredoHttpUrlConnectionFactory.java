package com.softartisans.timberwolf.exchange;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.AuthenticationException;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

public class AlfredoHttpUrlConnectionFactory implements HttpUrlConnectionFactory
{
    private static final String HTTP_METHOD = "POST";
    private static final int TIMEOUT = 10000;
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String SOAP_CONTENT_TYPE = "text/xml";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    public HttpURLConnection newInstance(String address, byte[] request)
        throws HttpUrlConnectionCreationException
    {
        try
        {
            AuthenticatedURL.Token token = new AuthenticatedURL.Token();
            URL url = new URL(address);
            HttpURLConnection conn = new AuthenticatedURL().openConnection(url, token);

            conn.setRequestMethod(HTTP_METHOD);
            conn.setDoOutput(true);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestProperty(CONTENT_TYPE_HEADER, SOAP_CONTENT_TYPE);
            conn.setRequestProperty(CONTENT_LENGTH_HEADER, "" + request.length);
            conn.getOutputStream().write(request);
            return conn;
        }
        catch (AuthenticationException e)
        {
            throw new HttpUrlConnectionCreationException(
                "There was an error authenticating with the remote server.", e);
        }
        catch (MalformedURLException e)
        {
            throw new HttpUrlConnectionCreationException(
                "The given url was not properly formed.", e);
        }
        catch (ProtocolException e)
        {
            throw new HttpUrlConnectionCreationException(
                "There was a protocol error while creating and sending the request.", e);
        }
        catch (IOException e)
        {
            throw new HttpUrlConnectionCreationException(
                "There was an IO error while sending a request to the remote server.", e);
        }
    }
}
