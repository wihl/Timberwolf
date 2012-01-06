package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;

import java.util.Scanner;

import org.apache.xmlbeans.XmlException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;
import org.xmlsoap.schemas.soap.envelope.EnvelopeType;

/**
 * ExchangeService handles packing xmlbeans objects into a SOAP envelope,
 * sending them off to the Exchange server and then returning the xmlbeans
 * objects that come back.
 *
 * Note that all the service calls are performed synchronously.
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
     *
     * @param url A string representing the URL of the service endpoint for the Exchange server.
     */
    public ExchangeService(final String url)
    {
        this(url, new AlfredoHttpUrlConnectionFactory());
    }

    /**
     * Sends a SOAP envelope request and returns the response.
     *
     * @param envelope An EnvelopeDocument with the SOAP envelope to send to Exchange.
     * @return An EnvelopeDocuemnt containing the SOAP envelope with Exchange's response.
     * @throws HttpErrorException If the HTTP response from Exchange has a non-200 status code.
     * @throws ServiceCallException If there was a non-HTTP error sending the response,
     *                              such as an improper encoding or IO error.
     */
    private EnvelopeDocument sendRequest(final EnvelopeDocument envelope)
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

        int amtAvailable;
        try
        {
            amtAvailable = responseData.available();
        }
        catch (IOException e)
        {
            LOG.error("Cannot determine the number of available bytes in the response.");
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error reading available bytes.");
        }


        if (code == HttpURLConnection.HTTP_OK)
        {
            if (amtAvailable == 0)
            {
                LOG.error("HTTP response was successful, but has no data.");
                if (!LOG.isTraceEnabled())
                {
                    LOG.debug("Request that generated the empty response:");
                    LOG.debug(request);
                }
                throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Response has empty body.");
            }

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
                // Why this works: http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
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

            if (amtAvailable > 0)
            {
                LOG.debug("Error response body:");
                // Why this works: http://weblogs.java.net/blog/pat/archive/2004/10/stupid_scanner_1.html
                LOG.debug(new Scanner(responseData).useDelimiter("\\A").next());
            }

            throw new HttpErrorException(code);
        }
    }

    /**
     * Returns the results of a find item request.
     *
     * @param findItem A FindItemType object that specifies the set of items to
     *                 gather from the Exchange server.
     * @return A FindItemResponseType object with the requested items.
     * @throws HttpErrorException If the HTTP response from Exchange has a non-200 status code.
     * @throws ServiceCallException If there was a non-HTTP error sending the response,
     *                              such as an improper encoding or IO error.
     */
    public FindItemResponseType findItem(final FindItemType findItem)
        throws ServiceCallException, HttpErrorException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setFindItem(findItem);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getFindItemResponse();
    }

    /**
     * Returns the results of a get item request.
     *
     * @param getItem A GetItemType object that specifies the set of items to
     *                gather from the Exchange server.
     * @return A GetItemResponseType object with the requested items.
     * @throws HttpErrorException If the HTTP response from Exchange has a non-200 status code.
     * @throws ServiceCallException If there was a non-HTTP error sending the response,
     *                              such as an improper encoding or IO error.
     */
    public GetItemResponseType getItem(final GetItemType getItem)
        throws ServiceCallException, HttpErrorException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setGetItem(getItem);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getGetItemResponse();
    }

    /**
     * Returns the response of a FindFolder request.
     * @param findFolder The FindFolder request,
     * @return The response.
     * @throws ServiceCallException A non-HTTP error has occurred during the request.
     * @throws HttpErrorException A HTTP error has occurred during the request.
     */
    public FindFolderResponseType findFolder(final FindFolderType findFolder)
        throws ServiceCallException, HttpErrorException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setFindFolder(findFolder);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getFindFolderResponse();
    }
}
