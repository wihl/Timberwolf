package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticatedURL;
import com.cloudera.alfredo.client.AuthenticationException;
import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * This is the MailStore implementation for Exchange email
 * This uses the Exchange web services to access
 */
public class ExchangeMailStore implements MailStore
{
    private static final Logger log = LoggerFactory.getLogger(
            ExchangeMailStore.class);
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
     */
    private static final int MaxGetItemsEntries = 50;

    /**
     * The service that does the actual sending of soap packages to exchange
     */
    private final ExchangeService exchangeService;

    public ExchangeMailStore(String exchangeUrl)
            throws IOException, UnsupportedEncodingException,
                   AuthenticationException
    {
        exchangeService = new ExchangeService(exchangeUrl);
    }

    private static FindItemType getFindItemsRequest(
            int offset, DistinguishedFolderIdNameType.Enum folder)
            throws UnsupportedEncodingException
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId =
                findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(folder);
        // TODO paging
        return findItem;
    }

    private static GetItemType getGetItemsRequest(List<String> ids)
            throws UnsupportedEncodingException
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        for (String id : ids)
        {
            items.addNewItemId().setId(id);
        }
        return getItem;
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
                return new EmailIterator(exchangeService);
            }
        };
    }

    private static class EmailIterator implements Iterator<MailboxItem>
    {
        Vector<String> currentIds;
        int currentIdIndex = 0;
        int findItemsOffset = 0;
        private Vector<MailboxItem> mailBoxItems;
        private int currentMailboxItemIndex = 0;
        private final ExchangeService exchangeService;

        private EmailIterator(ExchangeService exchangeService)
        {
            this.exchangeService = exchangeService;
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

        private static Vector<String> findItems(
                int offset, ExchangeService exchangeService)
                throws IOException, AuthenticationException, XmlException
        {
            FindItemResponseType response =
                    exchangeService.findItem(getFindItemsRequest(
                            offset, DistinguishedFolderIdNameType.INBOX));
            ArrayOfResponseMessagesType array =
                    response.getResponseMessages();
            Vector<String> items = new Vector<String>();
            for (FindItemResponseMessageType message : array
                    .getFindItemResponseMessageArray())
            {
                log.debug(message.getResponseCode().toString());
                for (MessageType item : message.getRootFolder().getItems()
                                               .getMessageArray())
                {
                    items.add(item.getItemId().getId());
                }
            }
            return items;
        }

        /**
         * Get a list of items from the server
         *
         * @param count the number of items to get.
         * if startIndex+count > ids.size() then only ids.size()-startIndex
         * items will be returned
         * @param startIndex the index in ids of the first item to get
         * @param ids a list of the ids to get
         * @return
         */
        private static Vector<MailboxItem> getItems(int count, int startIndex,
                                                    Vector<String> ids,
                                                    ExchangeService exchangeService)
                throws IOException, AuthenticationException, XmlException
        {
            int max = Math.min(startIndex + count, ids.size());
            if (max < startIndex)
            {
                return new Vector<MailboxItem>();
            }
            GetItemResponseType response = exchangeService
                    .getItem(getGetItemsRequest(ids.subList(startIndex, max)));
            ItemInfoResponseMessageType[] array =
                    response.getResponseMessages()
                            .getGetItemResponseMessageArray();
            Vector<MailboxItem> items = new Vector<MailboxItem>();
            for (ItemInfoResponseMessageType message : array)
            {
                for (MessageType item : message.getItems()
                                               .getMessageArray())
                {
                    items.add(new ExchangeEmail(item));
                }

            }
            return items;
        }


        @Override
        public boolean hasNext()
        {
            if (currentIds == null)
            {
                try
                {
                    currentMailboxItemIndex = 0;
                    currentIdIndex = 0;
                    currentIds = findItems(findItemsOffset, exchangeService);
                    log.debug("Got " + currentIds.size() + " email ids");
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
            // TODO paging here
            if (currentIdIndex >= currentIds.size())
            {
                return false;
            }
            if (mailBoxItems == null)
            {
                try
                {
                    currentMailboxItemIndex = 0;
                    mailBoxItems = getItems(MaxGetItemsEntries, currentIdIndex, currentIds, exchangeService);
                    log.debug("Got " + mailBoxItems.size() + " emails");
                    return currentMailboxItemIndex < mailBoxItems.size();
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
            }
            // TODO call getItems more than once
            return currentMailboxItemIndex < mailBoxItems.size();
        }

        @Override
        public MailboxItem next()
        {
            if (currentMailboxItemIndex < mailBoxItems.size())
            {
                MailboxItem item = mailBoxItems.get(currentMailboxItemIndex);
                currentMailboxItemIndex++;
                return item;
            }
            else
            {
                log.debug("All done, " + currentMailboxItemIndex + " >= " + mailBoxItems.size());
                return null;
            }
        }

        @Override
        public void remove()
        {
        }
    }

}
