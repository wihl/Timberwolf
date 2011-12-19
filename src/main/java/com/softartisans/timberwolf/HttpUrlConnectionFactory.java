package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;

import java.io.IOException;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;

interface HttpUrlConnectionFactory
{
    public HttpURLConnection newInstance(String address, byte[] request)
        throws MalformedURLException, IOException, ProtocolException,
               AuthenticationException;
}
