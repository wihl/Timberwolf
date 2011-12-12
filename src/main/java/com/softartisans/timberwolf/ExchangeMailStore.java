package com.softartisans.timberwolf;

import java.io.IOException;

import java.util.Iterator;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

/**
 * ExchangeMailStore represents a remote Exchange mail store.  It uses the
 * Exchange Web Services API to communicate with the Exchange server.
 */
public class ExchangeMailStore implements MailStore
{
    private String user;
    private String password;

    /**
     * Constructor that takes authentication information and a service endpoint.
     *
     * @param user The name of the user that ExchangeMailStore will authenticate
     * to the server with.
     *
     * @param password The password of the user that ExchangeMailStore will
     * authenticate to the server with.
     *
     * @param endpoint The location of the service endpoint that
     * ExchangeMailStore will connect to.
     */
    public ExchangeMailStore(String user, String password, String endpoint)
    {
        this.user = user;
        this.password = password;
    }

    public Iterator<MailboxItem> getMail(String user)
    {
        DefaultHttpClient client = new DefaultHttpClient();
        try
        {
            client.getCredentialsProvider().setCredentials(
                new AuthScope("localhost", 443),
                new UsernamePasswordCredentials(this.user, password));
            HttpGet get = new HttpGet("https://devexch01.int.tartarus.com/ews/exchange.asmx");
            System.out.println("executing request" + get.getRequestLine());
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            System.out.println("-------------------------------------");
            System.out.println(response.getStatusLine());
            if (entity != null)
            {
                System.out.println("Response content length: " + entity.getContentLength());
            }
        }
        catch (IOException e)
        {
            System.out.println("IO Exception: " + e.getMessage());
        }
        finally
        {
            client.getConnectionManager().shutdown();
        }

        return null;
    }
}
