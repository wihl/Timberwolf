package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.AuthenticationException;
import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import org.apache.xmlbeans.XmlException;
import org.apache.xmlbeans.XmlOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;
import org.xmlsoap.schemas.soap.envelope.EnvelopeType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
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
     * This should evenly divide MaxFindItemEntries
     * TODO make this larger
     */
    private static final int MaxGetItemsEntries = 5;

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

    private static byte[] getFindItemsRequest(int offset,
            DistinguishedFolderIdNameType.Enum folder)
            throws UnsupportedEncodingException
    {
        EnvelopeDocument envelopeDocument = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = envelopeDocument.addNewEnvelope();
        FindItemType findItem = envelope.addNewBody().addNewFindItem();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId =
                findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(folder);
        // TODO paging

        String request = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + envelopeDocument
                .xmlText();
        //request = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n               xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\">\n    <soap:Body>\n        <FindItem xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\"\n                  xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\"\n                  Traversal=\"Shallow\">\n            <ItemShape>\n                <t:BaseShape>IdOnly</t:BaseShape>\n            </ItemShape>\n            <ParentFolderIds>\n                <t:DistinguishedFolderId Id=\"inbox\"/>\n            </ParentFolderIds>\n        </FindItem>\n    </soap:Body>\n</soap:Envelope>";
        System.out.println(request);
        return request.getBytes("UTF-8");
    }

    private static byte[] getGetItemsRequest(Vector<String> ids)
            throws UnsupportedEncodingException
    {
        EnvelopeDocument envelopeDocument = EnvelopeDocument.Factory.newInstance();
        EnvelopeType envelope = envelopeDocument.addNewEnvelope();
        GetItemType getItem = envelope.addNewBody().addNewGetItem();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        ItemIdType itemId = items.addNewItemId();
        itemId.setId(ids.get(0));
        // TODO can we set more than one here??
        String request = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + envelopeDocument
                .xmlText();
        System.out.println(request);
        return request.getBytes("UTF-8");
    }

    @Override
    public Iterable<MailboxItem> getMail(String user)
            throws IOException, AuthenticationException
    {
        return new Iterable<MailboxItem>()
        {
            @Override
            public Iterator<MailboxItem> iterator()
            {
                return new EmailIterator(exchangeUrl);
            }
        };
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

        private static HttpURLConnection makeRequest(String exchangeUrl,
                                                     byte[] request)
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
            conn.setRequestProperty("Content-Length", "" + request.length);
            conn.getOutputStream().write(request);
            return conn;
        }

        private static Vector<String> findItems(int offset, String exchangeUrl)
                throws IOException, AuthenticationException, XmlException
        {
            byte[] bytes = getFindItemsRequest(offset,
                                               DistinguishedFolderIdNameType.INBOX);
            HttpURLConnection conn = makeRequest(exchangeUrl, bytes);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                EnvelopeDocument doc =
                        EnvelopeDocument.Factory.parse(conn.getInputStream());
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println(doc.xmlText());
                ArrayOfResponseMessagesType array =
                        doc.getEnvelope().getBody().getFindItemResponse()
                           .getResponseMessages();
                for (FindItemResponseMessageType message : array.getFindItemResponseMessageArray())
                {
                    log.debug(message.getResponseCode().toString());
                    Vector<String> items = new Vector<String>();
                    for (MessageType item : message.getRootFolder().getItems().getMessageArray())
                    {
                        items.add(item.getItemId().getId());
                    }
                    return items;
                }
                System.out.println(doc.xmlText());
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

        /**
         * Get a list of items from the server
         * @param count the number of items to get.
         * if startIndex+count > ids.size() then only ids.size()-startIndex
         * items will be returned
         * @param startIndex the index in ids of the first item to get
         * @param ids a list of the ids to get
         * @return
         */
        private static Vector<MailboxItem> getItems(int count, int startIndex, Vector<String> ids, String exchangeUrl)
                throws IOException, AuthenticationException, XmlException
        {
            byte[] bytes = getGetItemsRequest(ids);
            HttpURLConnection conn = makeRequest(exchangeUrl, bytes);

            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                EnvelopeDocument doc =
                        EnvelopeDocument.Factory.parse(conn.getInputStream());
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println();
                System.out.println(doc.xmlText());
                ArrayOfResponseMessagesType array =
                        doc.getEnvelope().getBody().getGetItemResponse().getResponseMessages();
                System.out.println(doc.xmlText());
                // TODO: parse response
                return new Vector<MailboxItem>();
            }
            else
            {
                log.error("Failed to find items; Status code: "
                          + conn.getResponseCode() + " "
                          + conn.getResponseMessage());
            }

            return new Vector<MailboxItem>();
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
                catch (XmlException e)
                {
                    log.error("findItems could not decode response", e);
                    return false;
                }
            }
            // TODO change this back
            return currentIdIndex < 1;//currentIds.size();
        }

        @Override
        public MailboxItem next()
        {
            if (currentIds == null)
            {
                return null;
            }
            Vector<MailboxItem> items = null;
            try
            {
                items = getItems(1, currentIdIndex, currentIds, exchangeUrl);
                currentIdIndex++;
                if (items.size() > 0)
                {
                    return items.get(0);
                }
            }
            catch (IOException e)
            {
                log.error("getItems failed", e);
            }
            catch (AuthenticationException e)
            {
                log.error("getItems could not authenticate", e);
            }
            catch (XmlException e)
            {
                log.error("getItems could not decode response", e);
            }
            return null;
        }

        @Override
        public void remove()
        {
        }
    }

}
