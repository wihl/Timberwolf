package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.ByteArrayInputStream;

/** 
 * An http connection factory that returns mock connections.  These can be used
 * for testing.
 */
public class MockHttpUrlConnectionFactory implements HttpUrlConnectionFactory
{
    List<MockRequest> requests = new ArrayList<MockRequest>();

    public MockRequest forRequest(String address, byte[] request)
    {
        return new MockRequest(address, request);
    }

    public HttpURLConnection newInstance(String address, byte[] request)
        throws MalformedURLException, IOException, ProtocolException,
               AuthenticationException
    {
        for (MockRequest mockRequest : requests)
        {
            if (mockRequest.url == address &&
                Arrays.equals(mockRequest.requestData, request))
            {
                HttpURLConnection mockConn = mock(HttpURLConnection.class);
                when(mockConn.getResponseCode()).thenReturn(mockRequest.code);
                when(mockConn.getInputStream()).thenReturn(
                    new ByteArrayInputStream(mockRequest.responseData));
                return mockConn;
            }
        }

        //FIXME
        throw new ProtocolException("No mocked request matched that URL and request data.");
    }

    public class MockRequest
    {
        private String url;
        private byte[] requestData;
        private int code;
        private byte[] responseData;

        public MockRequest(String address, byte[] request)
        {
            url = address;
            requestData = request;
        }

        public void respondWith(int status, byte[] response)
        {
            code = status;
            responseData = response;

            requests.add(this);
        }
    }
}
