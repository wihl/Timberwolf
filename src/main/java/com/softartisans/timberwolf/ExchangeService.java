package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.AuthenticationException;

import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;

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
    private static final String DECLARATION =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>";
    private static final String HTTP_METHOD = "POST";
    private static final int TIMEOUT = 10000;
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String SOAP_CONTENT_TYPE = "text/xml";
    private static final String CONTENT_LENGTH_HEADER = "Content-Length";
    private static final String SOAP_ENCODING = "UTF-8";

    private String endpoint;

    /** 
     * Creates a new ExchangeService that talks to the given Exchange server.
     */    
    public ExchangeService(String url)
    {
        endpoint = url;
    }

    /** 
     * Creates a new HTTP connection to the exchange server that will deliver
     * the given request.
     */
    private HttpURLConnection makeRequest(byte[] request)
        throws MalformedURLException, IOException, ProtocolException,
               AuthenticationException
    {
        AuthenticatedURL.Token token = new AuthenticatedURL.Token();
        URL url = new URL(endpoint);
        
        HttpURLConnection conn = new AuthenticatedURL().openConnection(url, 
                                                                       token);
        conn.setRequestMethod(HTTP_METHOD);
        conn.setDoOutput(true);
        conn.setReadTimeout(TIMEOUT);
        conn.setRequestProperty(CONTENT_TYPE_HEADER, SOAP_CONTENT_TYPE);
        conn.setRequestProperty(CONTENT_LENGTH_HEADER, "" + request.length);
        conn.getOutputStream().write(request);
        return conn;
    }

    /** Sends a SOAP envelope request and returns the response. */
    private EnvelopeDocument sendRequest(EnvelopeDocument envelope)
        throws UnsupportedEncodingException, IOException, XmlException,
               AuthenticationException
    {
        String request = DECLARATION + envelope.xmlText();
        // TODO: log request.

        HttpURLConnection conn = makeRequest(request.getBytes(SOAP_ENCODING));
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            EnvelopeDocument response = EnvelopeDocument.Factory.parse(
                conn.getInputStream());
            return response;
        }
        else
        {
            // TODO: log error
            // And return something better.
            return EnvelopeDocument.Factory.newInstance();
        }
    }

    /** Returns the results of a find item request. */
    public FindItemResponseType findItem(FindItemType findItem)
        throws UnsupportedEncodingException, IOException, XmlException,
               AuthenticationException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setFindItem(findItem);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getFindItemResponse();
    }

    /** Returns the results of a get item request. */
    public GetItemResponseType getItem(GetItemType getItem)
        throws UnsupportedEncodingException, IOException, XmlException,
               AuthenticationException
    {
        EnvelopeDocument request = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = request.addNewEnvelope();
        envelope.addNewBody().setGetItem(getItem);

        EnvelopeDocument response = sendRequest(request);
        return response.getEnvelope().getBody().getGetItemResponse();
    }
}
