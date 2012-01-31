/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
    private List<MockRequest> requests = new ArrayList<MockRequest>();

    public MockRequest forRequest(final String address, final byte[] request)
    {
        return new MockRequest(address, request);
    }

    public HttpURLConnection newInstance(final String address, final byte[] request) throws ServiceCallException
    {
        try
        {
            for (MockRequest mockRequest : requests)
            {
                if (mockRequest.url.equals(address) && Arrays.equals(mockRequest.requestData, request))
                {
                    HttpURLConnection mockConn = mock(HttpURLConnection.class);
                    when(mockConn.getResponseCode()).thenReturn(mockRequest.code);
                    when(mockConn.getInputStream()).thenReturn(new ByteArrayInputStream(mockRequest.responseData));
                    return mockConn;
                }
            }
        }
        catch (IOException e)
        {
            throw new ServiceCallException(ServiceCallException.Reason.OTHER,
                "There was an IO exception while mocking the request.", null);
        }

        throw new ServiceCallException(ServiceCallException.Reason.OTHER,
            "There was no mocked request matching the given url and data.", null);
    }

    /**
     * Represents a mock request.
     */
    public class MockRequest
    {
        private final String url;
        private final byte[] requestData;
        private int code;
        private byte[] responseData;

        public MockRequest(final String address, final byte[] request)
        {
            url = address;
            requestData = request;
        }

        public void respondWith(final int status, final byte[] response)
        {
            code = status;
            responseData = response;

            requests.add(this);
        }
    }
}
