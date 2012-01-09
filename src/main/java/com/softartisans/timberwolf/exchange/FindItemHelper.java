package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexBasePointType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexedPageViewType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Vector;

/**
 * Contains helper methods for FindItems requests.
 */
public class FindItemHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(FindItemHelper.class);

    /**
     * This is the side of the search results to start paging at.
     * I'm not sure which one is the earliest or latest yet, but the options
     * are "beginning" or "end"
     */
    private static final IndexBasePointType.Enum FIND_ITEMS_BASE_POINT = IndexBasePointType.BEGINNING;

    /**
     * Creates a FindItemType to request all the ids for the given distinguished folder.
     *
     * @param folder the distinguished folder from which to get ids
     * @param offset the number of entries to start from
     * @param maxEntries the maximum number of ids to grab with this request
     * @return the FindItemType necessary to request the ids
     */
    static FindItemType getFindItemsRequest(final DistinguishedFolderIdNameType.Enum folder, final int offset,
                                            final int maxEntries)
    {
        FindItemType findItem = getPagingFindItemType(offset, maxEntries);

        DistinguishedFolderIdType folderId = findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(folder);

        return findItem;
    }

    /**
     * Creates a FindItemType to request all the ids for the given folder.
     *
     * @param folderId the folder from which to get ids
     * @param offset the number of entries to start from
     * @param maxEntries the maximum number of ids to grab with this request
     * @return the FindItemType necessary to request the ids
     */
    static FindItemType getFindItemsRequest(final String folderId, final int offset, final int maxEntries)
    {
        FindItemType findItem = getPagingFindItemType(offset, maxEntries);

        FolderIdType folder = findItem.addNewParentFolderIds().addNewFolderId();
        folder.setId(folderId);

        return findItem;
    }

    /**
     * Creates a FindItemType with a default behavior and preset for specified paging properties.
     * @param offset the number of entries to start from
     * @param maxEntries the maximum number of ids to grab with this request
     * @return a FindItemType with the specified attributes
     */
    private static FindItemType getPagingFindItemType(final int offset, final int maxEntries)
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);

        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        // Asking for negative or zero max items is nonsensical.
        index.setMaxEntriesReturned(Math.max(maxEntries, 1));
        index.setBasePoint(FIND_ITEMS_BASE_POINT);
        // Negative offsets are nonsensical.
        index.setOffset(Math.max(offset, 0));

        return findItem;
    }

    /**
     * Gets a list of ids from a specified distinguished folder for the current user.
     *
     * @param exchangeService the actual service to use when requesting ids
     * @param folder the distinguished folder to obtain ids for
     * @param offset the number of items to offset for paging
     * @param maxEntries the maximum number of entries to add
     * @return a list of exchange ids
     * @throws ServiceCallException If we can't connect to the exchange service
     * @throws HttpErrorException If the response cannot be parsed
     */
    static Vector<String> findItems(final ExchangeService exchangeService,
                                    final DistinguishedFolderIdNameType.Enum folder,
                                    final int offset, final int maxEntries)
            throws ServiceCallException, HttpErrorException
    {
        return findItems(exchangeService, getFindItemsRequest(folder, offset, maxEntries));
    }

    /**
     * Gets a list of ids for a specified folder by folder id for the current user.
     *
     * @param exchangeService the actual service to use when requesting ids
     * @param folderId the folder id to look inside
     * @param offset the number of items to offset for paging
     * @param maxEntries the maximum number of entries to add
     * @return a list of exchange ids
     * @throws ServiceCallException If we can't connect to the exchange service
     * @throws HttpErrorException If the response cannot be parsed
     */
    static Vector<String> findItems(final ExchangeService exchangeService, final String folderId, final int offset,
                                    final int maxEntries)
            throws ServiceCallException, HttpErrorException
    {
        return findItems(exchangeService, getFindItemsRequest(folderId, offset, maxEntries));
    }

    /**
     * Gets a list of ids for a specified folder by FindItemType for the current user.
     *
     * @param exchangeService the actual service to use when requesting ids
     * @param findItem the FindItemType to use for the request
     * @return a list of exchange ids
     * @throws ServiceCallException If we can't connect to the exchange service
     * @throws HttpErrorException If the response cannot be parsed
     */
    private static Vector<String> findItems(final ExchangeService exchangeService, final FindItemType findItem)
            throws ServiceCallException, HttpErrorException
    {
        FindItemResponseType response = exchangeService.findItem(findItem);

        if (response == null)
        {
            LOG.debug("Exchange service returned null find item response.");
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Null response from Exchange service.");
        }

        ArrayOfResponseMessagesType array = response.getResponseMessages();
        Vector<String> items = new Vector<String>();
        for (FindItemResponseMessageType message : array.getFindItemResponseMessageArray())
        {
            ResponseCodeType.Enum errorCode = message.getResponseCode();
            if (errorCode != null && errorCode != ResponseCodeType.NO_ERROR)
            {
                LOG.debug(errorCode.toString());
                throw new ServiceCallException(errorCode, "SOAP response contained an error.");
            }

            for (MessageType item : message.getRootFolder().getItems().getMessageArray())
            {
                items.add(item.getItemId().getId());
            }
        }
        return items;
    }
}
