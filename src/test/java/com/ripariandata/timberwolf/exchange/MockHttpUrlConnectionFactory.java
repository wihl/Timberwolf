package com.ripariandata.timberwolf.exchange;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    public HttpURLConnection newInstance(String address, byte[] request) throws ServiceCallException
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
            throw new ServiceCallException(ServiceCallException.Reason.OTHER,
                "There was an IO exception while mocking the request.", null);
        }

        throw new ServiceCallException(ServiceCallException.Reason.OTHER,
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
