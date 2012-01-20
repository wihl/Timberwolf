package com.softartisans.timberwolf.exchange;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A HttpUrlConnectionFactory that works with Sasl.
 */
class SaslHttpUrlConnectionFactory implements HttpUrlConnectionFactory
{
    private static final Logger LOG = LoggerFactory.getLogger(SaslHttpUrlConnectionFactory.class);

    private static final String HTTP_METHOD = "POST";
    private static final int TIMEOUT = 10000;
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String SOAP_CONTENT_TYPE = "text/xml";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";

    public HttpURLConnection newInstance(final String address, final byte[] request) throws ServiceCallException
    {
        try
        {
            URL url = new URL(address);
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();

            conn.setRequestMethod(HTTP_METHOD);
            conn.setDoOutput(true);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestProperty(CONTENT_TYPE_HEADER, SOAP_CONTENT_TYPE);
            conn.setRequestProperty(CONTENT_LENGTH_HEADER, "" + request.length);
            conn.getOutputStream().write(request);
            return conn;
        }
        catch (MalformedURLException e)
        {
            LOG.error("Improperly formed URL " + address, e);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER,
                "The given url was not properly formed.", e);
        }
        catch (ProtocolException e)
        {
            LOG.error("Protocol exception when contacting URL " + address + " with request " + new String(request), e);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER,
                "There was a protocol error while creating and sending the request.", e);
        }
        catch (IOException e)
        {
            LOG.error("IO exception when contacting URL " + address + " with request " + new String(request), e);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER,
                "There was an IO error while sending a request to the remote server.", e);
        }
    }
}
