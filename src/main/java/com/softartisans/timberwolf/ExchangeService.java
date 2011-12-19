package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;
import org.xmlsoap.schemas.soap.envelope.EnvelopeType;
import java.net.URL;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.io.IOException;
import java.net.ProtocolException;
import java.io.UnsupportedEncodingException;
import org.apache.xmlbeans.XmlException;
import com.cloudera.alfredo.client.AuthenticationException;

/**
 * ExchangeService handles packing xmlbeans objects into a SOAP envelope,
 * sending them off to the Exchange server and then returning the xmlbeans
 * objects that come back.
 */
public class ExchangeService
{
    private static final String declaration =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>";

    private String endpoint;

    public ExchangeService(String url)
    {
        endpoint = url;
    }

    private HttpURLConnection makeRequest(byte[] request)
        throws MalformedURLException, IOException, ProtocolException,
               AuthenticationException
    {
        AuthenticatedURL.Token token = new AuthenticatedURL.Token();
        URL url = new URL(endpoint);
        
        HttpURLConnection conn = new AuthenticatedURL().openConnection(url, token);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setRequestProperty("Content-Length", "" + request.length);
        conn.getOutputStream().write(request);
        return conn;
    }

    public FindItemResponseType findItem(FindItemType findItem)
        throws UnsupportedEncodingException, IOException, XmlException,
               AuthenticationException
    {
        EnvelopeDocument envelopeDoc = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = envelopeDoc.addNewEnvelope();
        envelope.addNewBody().setFindItem(findItem);
        String request = declaration + envelopeDoc.xmlText();
        // TODO: log request
        
        HttpURLConnection conn = makeRequest(request.getBytes("UTF-8"));
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK)
        {
            EnvelopeDocument responseDoc = EnvelopeDocument.Factory.parse(conn.getInputStream());
            // TODO: Check for error response.
            return responseDoc.getEnvelope().getBody().getFindItemResponse();
        }
        else
        {
            // TODO: log error
            return FindItemResponseType.Factory.newInstance();
        }
    }
}
