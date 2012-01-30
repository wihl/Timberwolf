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
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

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
            throw ServiceCallException.log(LOG, new ServiceCallException(ServiceCallException.Reason.OTHER,
                    "Improperly formed URL " + address, e));
        }
        catch (ProtocolException e)
        {
            throw ServiceCallException.log(LOG, new ServiceCallException(ServiceCallException.Reason.OTHER,
                    "Protocol exception when contacting URL " + address + " with request " + new String(request), e));
        }
        catch (IOException e)
        {
            throw ServiceCallException.log(LOG, new ServiceCallException(ServiceCallException.Reason.OTHER,
                    "IO exception when contacting URL " + address + " with request " + new String(request), e));
        }
    }
}
