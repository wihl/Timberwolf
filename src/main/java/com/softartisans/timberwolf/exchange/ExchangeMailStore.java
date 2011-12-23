package com.softartisans.timberwolf.exchange;

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
import com.softartisans.timberwolf.MailStore;
import com.softartisans.timberwolf.MailboxItem;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.xmlbeans.XmlException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the MailStore implementation for Exchange email.
 * This uses the Exchange web services to access exchange and get back email
 * items.
 */
public class ExchangeMailStore implements MailStore
{
    private static final Logger LOG = LoggerFactory.getLogger(
            ExchangeMailStore.class);
    /*
     * I'm leaving this here for when we get to doing paging. If we decide
     * not to do paging these variables and notes should probably be removed
     *
     *
     *
     *
     * When FindItems is run, you can limit the number of items to get at a time
     * and page, starting with 1000, but we'll probably want to profile this a
     * bit to figure out if we want more or less
     *
     private static final int MaxFindItemEntries = 1000;

     /**
     * This is the side of the search results to start paging at.
     * I'm not sure which one is the earliest or latest yet, but the options
     * are "beginning" or "end"
     * TODO change this to an actual enum from our xml binding
     *
     private static final String FindItemsBasePoint = "Beginning";
     */

    /**
     * GetItems takes multiple ids, but we don't want to call GetItems on all
     * MaxFindItemEntries at a time, because those could be massive responses
     * Instead, get a smaller number at a time.
     * This should evenly divide MaxFindItemEntries
     */
    private static final int MAX_GET_ITEMS_ENTRIES = 50;

    /** The service that does the sending of soap packages to exchange. */
    private final ExchangeService exchangeService;

    /**
     * Creates a new ExchangeMailStore for getting mail from the exchange
     * server at the provided url.
     *
     * @param exchangeUrl the url to the exchange web service such as
     * https://devexch01.int.tartarus.com/ews/exchange.asmx
     */
    public ExchangeMailStore(final String exchangeUrl)
    {
        exchangeService = new ExchangeService(exchangeUrl);
    }

    /**
     * Creates a new ExchangeMailStore for getting mail.
     * @param service The exchange service to use
     */
    ExchangeMailStore(final ExchangeService service)
    {
        this.exchangeService = service;
    }

    /**
     * Creates a FindItemType to request all the ids for the given folder.
     *
     * @param folder the folder from which to get ids
     * @return the FindItemType necessary to request the ids
     */
    static FindItemType getFindItemsRequest(
            final DistinguishedFolderIdNameType.Enum folder)
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId =
                findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(folder);
        // TODO paging - put off until HAM-78
        return findItem;
    }

    /**
     * Gets a list of ids for the inbox for the current user.
     *
     * @param exchangeService the actual service to use when requesting ids
     * @return a list of exchange ids
     * @throws AuthenticationException If we can't authenticate to the
     * exchange service
     * @throws IOException If we can't connect to the exchange service
     * @throws XmlException If the response cannot be parsed
     * @throws HttpUrlConnectionCreationException if it failed to create a
     * connection to the service
     */
    static Vector<String> findItems(
            final ExchangeService exchangeService)
            throws IOException, AuthenticationException, XmlException,
                   HttpUrlConnectionCreationException
    {
        FindItemResponseType response =
                exchangeService.findItem(getFindItemsRequest(
                        DistinguishedFolderIdNameType.INBOX));
        ArrayOfResponseMessagesType array =
                response.getResponseMessages();
        Vector<String> items = new Vector<String>();
        for (FindItemResponseMessageType message : array
                .getFindItemResponseMessageArray())
        {
            LOG.debug(message.getResponseCode().toString());
            for (MessageType item : message.getRootFolder().getItems()
                                           .getMessageArray())
            {
                items.add(item.getItemId().getId());
            }
        }
        return items;
    }

    /**
     * Creates a GetItemType to request the info for the given ids.
     *
     * @param ids the ids to request
     * @return the GetItemType necessary to request the info for those ids
     */
    static GetItemType getGetItemsRequest(final List<String> ids)
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        if (ids != null)
        {
            for (String id : ids)
            {
                items.addNewItemId().setId(id);
            }
        }
        return getItem;
    }

    /**
     * Get a list of items from the server.
     *
     * @param count the number of items to get.
     * if startIndex+count > ids.size() then only ids.size()-startIndex
     * items will be returned
     * @param startIndex the index in ids of the first item to get
     * @param ids a list of ids to get
     * @param exchangeService the backend service used for contacting
     * exchange
     * @return a list of mailbox items that correspond to the ids in the
     *         In the list of ids
     * @throws AuthenticationException If we can't authenticate to the
     * exchange service
     * @throws IOException If we can't connect to the exchange service
     * @throws XmlException If the response cannot be parsed
     * @throws HttpUrlConnectionCreationException If it failed to connect
     * to the service
     */
    static Vector<MailboxItem> getItems(
            final int count, final int startIndex, final Vector<String> ids,
            final ExchangeService exchangeService)
            throws IOException, AuthenticationException, XmlException,
                   HttpUrlConnectionCreationException
    {
        int max = Math.min(startIndex + count, ids.size());
        if (max <= startIndex)
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
    public final Iterable<MailboxItem> getMail()
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

    /**
     * This Iterator will request a list of all ids from the exchange service
     * and then get actual mail items for those ids.
     */
    private static final class EmailIterator implements Iterator<MailboxItem>
    {
        private Vector<String> currentIds;
        private int currentIdIndex = 0;
        private int findItemsOffset = 0;
        private Vector<MailboxItem> mailBoxItems;
        private int currentMailboxItemIndex = 0;
        private final ExchangeService exchangeService;

        private EmailIterator(final ExchangeService service)
        {
            this.exchangeService = service;
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
                    currentIds = findItems(exchangeService);
                    LOG.debug("Got " + currentIds.size() + " email ids");
                }
                catch (IOException e)
                {
                    LOG.error("findItems failed to get ids", e);
                    return false;
                }
                catch (AuthenticationException e)
                {
                    LOG.error("findItems could not authenticate", e);
                    return false;
                }
                catch (XmlException e)
                {
                    LOG.error("findItems could not decode response", e);
                    return false;
                }
                catch (HttpUrlConnectionCreationException e)
                {
                    LOG.error("findItems failed to get ids", e);
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
                    mailBoxItems = getItems(MAX_GET_ITEMS_ENTRIES,
                                            currentIdIndex, currentIds,
                                            exchangeService);
                    LOG.debug("Got " + mailBoxItems.size() + " emails");
                    return currentMailboxItemIndex < mailBoxItems.size();
                }
                catch (IOException e)
                {
                    LOG.error("getItems failed", e);
                }
                catch (AuthenticationException e)
                {
                    LOG.error("getItems could not authenticate", e);
                }
                catch (XmlException e)
                {
                    LOG.error("getItems could not decode response", e);
                }
                catch (HttpUrlConnectionCreationException e)
                {
                    LOG.error("getItems failed", e);
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
                LOG.debug("All done, " + currentMailboxItemIndex + " >= "
                          + mailBoxItems.size());
                return null;
            }
        }

        @Override
        public void remove()
        {
        }
    }

}
