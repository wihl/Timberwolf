package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.AuthenticationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.Vector;

/**
 * This is the MailStore implementation for Exchange email
 */
public class ExchangeService implements MailStore
{
    private static final Logger log = LoggerFactory.getLogger(
            ExchangeService.class);
    /**
     * When FindItems is run, you can limit the number of items to get at a time
     * and page, starting with 1000, but we'll probably want to profile this a
     * bit to figure out if we want more or less
     */
    private static final int MaxFindItemEntries = 1000;

    /**
     * This is the side of the search results to start paging at.
     * I'm not sure which one is the earliest or latest yet, but the options
     * are "beginning" or "end"
     * TODO change this to an actual enum from our xml binding
     */
    private static final String FindItemsBasePoint = "Beginning";

    /**
     * GetItems takes multiple ids, but we don't want to call GetItems on all
     * MaxFindItemEntries at a time, because those could be massive responses
     * Instead, get a smaller number at a time.
     */
    private static final int MaxGetItemsEntries = 50;

    /**
     * The url of the service, passed in as a command line parameter, or from a
     * config
     */
    private String exchangeUrl;

    public ExchangeService(String exchangeUrl)
            throws IOException, UnsupportedEncodingException,
                   AuthenticationException
    {
        this.exchangeUrl = exchangeUrl;
    }

    private static byte[] getFindItemsRequest(int offset, String folder)
            throws UnsupportedEncodingException
    {
        // TODO paging,and ask folder insertion
        String fi = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n               xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\">\n    <soap:Body>\n        <FindItem xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\"\n                  xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\"\n                  Traversal=\"Shallow\">\n            <ItemShape>\n                <t:BaseShape>IdOnly</t:BaseShape>\n            </ItemShape>\n            <ParentFolderIds>\n                <t:DistinguishedFolderId Id=\"inbox\"/>\n            </ParentFolderIds>\n        </FindItem>\n    </soap:Body>\n</soap:Envelope>";
        return fi.getBytes("UTF-8");
    }

    @Override
    public Iterator<MailboxItem> getMail(String user)
            throws IOException, AuthenticationException
    {
        return null;
    }

    private static class EmailIterator implements Iterator<MailboxItem>
    {
        Vector<String> currentIds;
        int currentIdIndex = 0;
        int findItemsOffset = 0;
        String exchangeUrl;

        private EmailIterator(String exchangeUrl)
        {
            this.exchangeUrl = exchangeUrl;
        }

        private static Vector<String> findItems(int offset, String exchangeUrl)
                throws IOException, AuthenticationException
        {
            AuthenticatedURL.Token token = new AuthenticatedURL.Token();
            URL url = new URL(exchangeUrl);
            HttpURLConnection conn = new AuthenticatedURL().openConnection(url,
                                                                           token);
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setReadTimeout(10000);
            conn.setRequestProperty("Content-Type", "text/xml");
            byte[] bytes = getFindItemsRequest(offset, "inbox");
            conn.setRequestProperty("Content-Length", "" + bytes.length);
            conn.getOutputStream().write(bytes);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line = reader.readLine();
                while (line != null) {
                    System.out.println(line);
                    line = reader.readLine();
                }
                reader.close();
                // TODO: parse response
                return new Vector<String>();
            }
            else
            {
                log.error("Failed to find items; Status code: "
                          + conn.getResponseCode() + " "
                          + conn.getResponseMessage());
            }

            return new Vector<String>();
        }

        @Override
        public boolean hasNext()
        {
            if (currentIds == null)
            {
                try
                {
                    currentIdIndex = 0;
                    currentIds = findItems(findItemsOffset, exchangeUrl);
                }
                catch (IOException e)
                {
                    log.error("findItems failed to get ids", e);
                    return false;
                }
                catch (AuthenticationException e)
                {
                    log.error("findItems could not authenticate", e);
                    return false;
                }
            }
            return currentIdIndex < currentIds.size();
        }

        @Override
        public MailboxItem next()
        {
            return null;
        }

        @Override
        public void remove()
        {
        }
    }

}
