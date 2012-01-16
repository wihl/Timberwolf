package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.ConstantValueType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.FieldURIOrConstantType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexBasePointType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexedPageViewType;
import com.microsoft.schemas.exchange.services.x2006.types.IsGreaterThanType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.PathToUnindexedFieldType;
import com.microsoft.schemas.exchange.services.x2006.types.RestrictionType;
import com.microsoft.schemas.exchange.services.x2006.types.UnindexedFieldURIType;

import java.util.Vector;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains helper methods for FindItems requests.
 */
public final class FindItemHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(FindItemHelper.class);

    /**
     * This is the side of the search results to start paging at.
     * I'm not sure which one is the earliest or latest yet, but the options
     * are "beginning" or "end"
     */
    private static final IndexBasePointType.Enum FIND_ITEMS_BASE_POINT = IndexBasePointType.BEGINNING;

    /**
     * Enforces not being able to create an instance.
     */
    private FindItemHelper()
    {

    }

    /**
     * Creates a FindItemType to request all the ids for the
     * given configuration. This limits the scope of the
     * search to the folder specified in the configuration.
     *
     * @param folderId the folder from which to get ids
     * @param offset the number of entries to start from
     * @param maxEntries the maximum number of ids to grab with this request
     * @return the FindItemType necessary to request the ids
     */
    static FindItemType getFindItemsRequest(final Configuration config,
                                            final FolderContext folder,
                                            final int offset)
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);

        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        index.setMaxEntriesReturned(config.getFindItemPageSize());
        index.setBasePoint(FIND_ITEMS_BASE_POINT);
        // Negative offsets are nonsensical.
        index.setOffset(Math.max(offset, 0));

        // TODO: Uncomment this once this work is combined with the
        // configuration material. getStartDate() will need to be created.
        /*
        DateTime startDate = config.getStartDate();
        if (startDate != null)
        {
            findItem.setRestriction(getAfterDateRestriction(startDate));
        }
        */
        
        findItem.setParentFolderIds(folder.getFolderIds());

        return findItem;
    }

    private static RestrictionType getAfterDateRestriction(final DateTime startDate)
    {
        IsGreaterThanType isGreaterThan = IsGreaterThanType.Factory.newInstance();

        PathToUnindexedFieldType dateSentPath = PathToUnindexedFieldType.Factory.newInstance();
        dateSentPath.setFieldURI(UnindexedFieldURIType.ITEM_DATE_TIME_SENT);
        isGreaterThan.setPath(dateSentPath);

        FieldURIOrConstantType dateConstraint = isGreaterThan.addNewFieldURIOrConstant();
        ConstantValueType dateConstantValue = dateConstraint.addNewConstant();
        dateConstantValue.setValue(startDate.toString());

        RestrictionType restriction = RestrictionType.Factory.newInstance();
        restriction.setSearchExpression(isGreaterThan);
        return restriction;
    }

    /**
     * Gets a list of ids from the folder specified
     * in the configuration for the current user.
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
                                    final Configuration config,
                                    final FolderContext folder,
                                    final int offset)
            throws ServiceCallException, HttpErrorException
    {
        return findItems(exchangeService,
                         getFindItemsRequest(config, folder, offset));
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

            if (message.isSetRootFolder() && message.getRootFolder().isSetItems())
            {
                for (MessageType item : message.getRootFolder().getItems().getMessageArray())
                {
                    if (item.isSetItemId())
                    {
                        items.add(item.getItemId().getId());
                    }
                }
            }
        }
        return items;
    }
}
