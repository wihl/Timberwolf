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
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlsoap.schemas.soap.envelope.BodyType;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;
import org.xmlsoap.schemas.soap.envelope.EnvelopeType;


import static com.softartisans.timberwolf.Utilities.inputStreamToString;

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
     *
     * @param envelope An EnvelopeDocument with the SOAP envelope to send to Exchange.
     * @return An SOAP body from the Exchange's response.
     * @throws HttpErrorException If the HTTP response from Exchange has a non-200 status code.
     * @throws ServiceCallException If there was a non-HTTP error sending the response,
     *                              such as an improper encoding or IO error.
     */
    private BodyType sendRequest(final EnvelopeDocument envelope)
        throws HttpErrorException, ServiceCallException
    {
        String request = DECLARATION + envelope.xmlText();
        LOG.trace("Sending SOAP request to {}.  SOAP envelope:", endpoint);
        LOG.trace(request);

        HttpURLConnection conn = createConnection(request);
        int code = getResponseCode(conn);

        InputStream responseData = getInputStream(conn);

        int amtAvailable = getAmountAvailable(responseData);

        if (code == HttpURLConnection.HTTP_OK)
        {
            checkNonEmptyResponse(request, amtAvailable);

            EnvelopeDocument response = parseResponse(responseData);
            LOG.trace("SOAP response received from {}.  SOAP envelope:", endpoint);
            LOG.trace(response.xmlText());
            return getSoapBody(response);
        }
        else
        {
            return logAndThrowHttpErrorCode(request, code, responseData, amtAvailable);
        }
    }

    private BodyType logAndThrowHttpErrorCode(final String request, final int code, final InputStream responseData,
                                              final int amtAvailable)
            throws ServiceCallException, HttpErrorException
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
            try
            {
                LOG.debug(inputStreamToString(responseData));
            }
            catch (IOException ioe)
            {
                LOG.error("There was an error reading from the response stream.", ioe);
                throw new ServiceCallException(ServiceCallException.Reason.OTHER,
                                               "Error reading response stream.", ioe);
            }
        }

        throw new HttpErrorException(code);
    }

    /**
     * Extracts the soap body from the response.
     * @param response the response from exchange
     * @return the soap body from the response
     * @throws ServiceCallException if there was an error extracting the body.
     * The error will be logged.
     */
    private BodyType getSoapBody(final EnvelopeDocument response) throws ServiceCallException
    {
        BodyType body = response.getEnvelope().getBody();
        if (body != null)
        {
            return body;
        }
        else
        {
            LOG.error("SOAP envelope did not contain a valid body");
            if (!LOG.isTraceEnabled())
            {
                LOG.error("SOAP envelope:");
                LOG.error(response.xmlText());
            }
            throw new ServiceCallException(ServiceCallException.Reason.OTHER,
                                           "SOAP response did not contain a body.");
        }
    }

    private void checkNonEmptyResponse(final String request, final int amtAvailable) throws ServiceCallException
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
    }

    private EnvelopeDocument parseResponse(final InputStream responseData) throws ServiceCallException
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
            try
            {
                LOG.debug(inputStreamToString(responseData));
                throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error parsing SOAP response.", e);
            }
            catch (IOException ioe)
            {
                LOG.error("There was an error reading from the response stream.", ioe);
                throw new ServiceCallException(ServiceCallException.Reason.OTHER,
                                               "Error reading response stream.", ioe);
            }
        }
        return response;
    }

    private int getAmountAvailable(final InputStream responseData) throws ServiceCallException
    {
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
        return amtAvailable;
    }

    private InputStream getInputStream(final HttpURLConnection conn) throws ServiceCallException
    {
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
        return responseData;
    }

    private int getResponseCode(final HttpURLConnection conn) throws ServiceCallException
    {
        int code;
        try
        {
            code = conn.getResponseCode();
        }
        catch (IOException e)
        {
            LOG.error("There was an error getting the HTTP status code for the response.", e);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error getting HTTP status code.", e);
        }
        return code;
    }

    private HttpURLConnection createConnection(final String request) throws ServiceCallException
    {
        HttpURLConnection conn;
        try
        {
            conn = connectionFactory.newInstance(endpoint, request.getBytes(SOAP_ENCODING));
        }
        catch (UnsupportedEncodingException e)
        {
            LOG.error("Request body could not be encoded into " + SOAP_ENCODING, e);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Error encoding request body.", e);
        }
        return conn;
    }

    EnvelopeDocument createEmptyRequest(final String targetUser)
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewHeader().addNewExchangeImpersonation().addNewConnectingSID().setPrincipalName(targetUser);
        return request;
    }

    /**
     * Returns the results of a find item request.
     *
     * @param findItem A FindItemType object that specifies the set of items to
     *                 gather from the Exchange server.
     * @param targetUser The principal name of the user to find items for.
     * @return A FindItemResponseType object with the requested items.
     * @throws HttpErrorException If the HTTP response from Exchange has a non-200 status code.
     * @throws ServiceCallException If there was a non-HTTP error sending the response,
     *                              such as an improper encoding or IO error.
     */
    public FindItemResponseType findItem(final FindItemType findItem, final String targetUser)
        throws ServiceCallException, HttpErrorException
    {
        EnvelopeDocument request = createEmptyRequest(targetUser);
        EnvelopeType envelope = request.getEnvelope();
        envelope.addNewBody().setFindItem(findItem);

        return sendRequest(request).getFindItemResponse();
    }

    /**
     * Returns the results of a get item request.
     *
     * @param getItem A GetItemType object that specifies the set of items to
     *                gather from the Exchange server.
     * @param targetUser The principal name of the user to get items for.
     * @return A GetItemResponseType object with the requested items.
     * @throws HttpErrorException If the HTTP response from Exchange has a non-200 status code.
     * @throws ServiceCallException If there was a non-HTTP error sending the response,
     *                              such as an improper encoding or IO error.
     */
    public GetItemResponseType getItem(final GetItemType getItem, final String targetUser)
        throws ServiceCallException, HttpErrorException
    {
        EnvelopeDocument request = createEmptyRequest(targetUser);
        EnvelopeType envelope = request.getEnvelope();
        envelope.addNewBody().setGetItem(getItem);

        return sendRequest(request).getGetItemResponse();
    }

    /**
     * Returns the response of a FindFolder request.
     * @param findFolder The FindFolder request,
     * @param targetUser The principal name of the user to find folders for.
     * @return The response.
     * @throws ServiceCallException A non-HTTP error has occurred during the request.
     * @throws HttpErrorException A HTTP error has occurred during the request.
     */
    public FindFolderResponseType findFolder(final FindFolderType findFolder, final String targetUser)
        throws ServiceCallException, HttpErrorException
    {
        EnvelopeDocument request = createEmptyRequest(targetUser);
        EnvelopeType envelope = request.getEnvelope();
        envelope.addNewBody().setFindFolder(findFolder);

        return sendRequest(request).getFindFolderResponse();
    }
}
