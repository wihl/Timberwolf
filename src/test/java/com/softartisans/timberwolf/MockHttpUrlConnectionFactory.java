package com.softartisans.timberwolf;

import java.net.HttpURLConnection;
import static org.mockito.Mockito.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.io.ByteArrayInputStream;
import java.io.IOException;

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
        throws HttpUrlConnectionCreationException
    {
        try
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
        }
        catch(IOException e)
        {
            throw new HttpUrlConnectionCreationException(
                "There was an IO exception while mocking the request.", null);            
        }        

        throw new HttpUrlConnectionCreationException(
            "There was no mocked request matching the given url and data.", null);
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
