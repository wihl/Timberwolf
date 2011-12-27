package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.util.Scanner;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.xmlbeans.XmlException;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;
import org.xmlsoap.schemas.soap.envelope.EnvelopeType;

/**
 * ExchangeService handles packing xmlbeans objects into a SOAP envelope,
 * sending them off to the Exchange server and then returning the xmlbeans
 * objects that come back.
 */
public class ExchangeService
{
    private static final Logger LOG = LoggerFactory.getLogger(ExchangeService.class);

    private static final String DECLARATION = "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    private static final String SOAP_ENCODING = "UTF-8";

    private String endpoint;
    private HttpUrlConnectionFactory connectionFactory;

    public ExchangeService(final String url, final HttpUrlConnectionFactory factory)
    {
        endpoint = url;
        connectionFactory = factory;
    }

    /**
     * Creates a new ExchangeService that talks to the given Exchange server.
     */
    public ExchangeService(final String url)
    {
        this(url, new AlfredoHttpUrlConnectionFactory());
    }

    /** Sends a SOAP envelope request and returns the response. */
    private EnvelopeDocument sendRequest(EnvelopeDocument envelope)
        throws HttpErrorException, ServiceCallException
    {
        String request = DECLARATION + envelope.xmlText();
        LOG.trace("Sending SOAP request to {}.  SOAP envelope:", endpoint);
        LOG.trace(request);

        HttpURLConnection conn;
        int code;
        try
        {
            conn = connectionFactory.newInstance(endpoint, request.getBytes(SOAP_ENCODING));
            code = conn.getResponseCode();
        }
        catch (UnsupportedEncodingException e)
        {
            LOG.error("Request body could not be encoded into " + SOAP_ENCODING, e);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error encoding request body.", e);
        }
        catch (IOException e)
        {
            LOG.error("There was an error getting the HTTP status code for the response.", e);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error getting HTTP status code.", e);
        }

        InputStream responseData;
        try
        {
           responseData = conn.getInputStream(); 
        }
        catch (IOException e)
        {
            LOG.error("There was an error getting the input stream for the response.", e);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error getting input stream.", e);
        }
        
        if (code == HttpURLConnection.HTTP_OK)
        {
            EnvelopeDocument response;
            try
            {
                response = EnvelopeDocument.Factory.parse(responseData);
            }
            catch (IOException e)
            {
                LOG.error("There was an error reading from the response stream.", e);
                throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error reading response stream.", e);
            }
            catch (XmlException e)
            {
                LOG.error("There was an error parsing the SOAP response from Exchange.");
                LOG.debug("Response body:");
                // Why this works: http://stackoverflow.com/questions/309424/in-java-how-do-i-read-convert-an-inputstream-to-a-string#5445161
                LOG.debug(new Scanner(responseData).useDelimiter("\\A").next());
                throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error parsing SOAP response.", e);
            }
            
            LOG.trace("SOAP response received from {}.  SOAP envelope:", endpoint);
            LOG.trace(response.xmlText());
            return response;
        }
        else
        {
            LOG.error("Server responded with HTTP error code {}.", code);
            if (!LOG.isTraceEnabled())
            {
                LOG.debug("Request that generated the error:");
                LOG.debug(request);
            }
            
            LOG.debug("Error response body:");
            // Why this works: http://stackoverflow.com/questions/309424/in-java-how-do-i-read-convert-an-inputstream-to-a-string#5445161
            LOG.debug(new Scanner(responseData).useDelimiter("\\A").next());            
            throw new HttpErrorException(code);
        }
    }

    /** Returns the results of a find item request. */
    public FindItemResponseType findItem(FindItemType findItem)
        throws ServiceCallException, HttpErrorException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setFindItem(findItem);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getFindItemResponse();
    }

    /** Returns the results of a get item request. */
    public GetItemResponseType getItem(GetItemType getItem)
        throws ServiceCallException, HttpErrorException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setGetItem(getItem);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getGetItemResponse();
    }
}
