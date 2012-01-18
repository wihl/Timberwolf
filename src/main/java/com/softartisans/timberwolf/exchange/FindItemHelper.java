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
import com.microsoft.schemas.exchange.services.x2006.types.TwoOperandExpressionType;
import com.microsoft.schemas.exchange.services.x2006.types.RestrictionType;
import com.microsoft.schemas.exchange.services.x2006.types.UnindexedFieldURIType;

import java.util.Vector;

import javax.xml.namespace.QName;

import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.softartisans.timberwolf.Utilities.ISO_8601_FORMAT;

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
        throws ServiceCallException
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);

        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        index.setMaxEntriesReturned(config.getFindItemPageSize());
        index.setBasePoint(FIND_ITEMS_BASE_POINT);
        // Negative offsets are nonsensical.
        index.setOffset(Math.max(offset, 0));

        DateTime startDate = config.getLastUpdated(folder.getUser());
        if (startDate != null)
        {
            findItem.setRestriction(getAfterDateRestriction(startDate));
        }

        findItem.setParentFolderIds(folder.getFolderIds());

        return findItem;
    }

    static RestrictionType getAfterDateRestriction(final DateTime startDate) throws ServiceCallException
    {
        /* The xml schema concerning restrictions uses some rather unconventional
           features (abstract types and substitution groups) that xmlbeans has some
           trouble with.  In theory, we should be able to use the 'substitute'
           mehtod to insert the correct derived type into our XmlBeans object where
           it's currently using an abstract type, but I can't get that work properly.
           Fortunately, parsing the restriction from a string sets everything
           correctly.  If you get the urge to try to refactor this to do things the
           right way with substitutions, these links may help:
                http://wiki.apache.org/xmlbeans/SubstGroupsFaq
                http://xmlbeans.apache.org/docs/2.4.0/reference/index.html
        */

        String xml =
              "<mes:Restriction xmlns:mes=\"http://schemas.microsoft.com/exchange/services/2006/messages\""
            + "                 xmlns:typ=\"http://schemas.microsoft.com/exchange/services/2006/types\">"
            + "  <typ:IsGreaterThan>"
            + "    <typ:FieldURI FieldURI=\"item:DateTimeRecieved\" />"
            + "    <typ:FieldURIOrConstant>"
            + "      <typ:Constant Value=\"" + startDate.toString(ISO_8601_FORMAT) + "\" />"
            + "    </typ:FieldURIOrConstant>"
            + "  </typ:IsGreaterThan>"
            + "</mes:Restriction>";

        try
        {
            return RestrictionType.Factory.parse(xml);
        }
        catch (XmlException e)
        {
            LOG.error("Unable to parse generated Restriction tag:");
            LOG.error(xml);
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Cannot construct restriction tag.");
        }
    }

    /**
     * Gets a list of ids from the folder specified
     * in the configuration for the current user.
     *
     * @param exchangeService the actual service to use when requesting ids
     * @param folder the distinguished folder to obtain ids for
     * @param offset the number of items to offset for paging
     * @param maxEntries the maximum number of entries to add
     * @param targetUser The user to impersonate for the Exchange FindItem request.
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
        return findItems(exchangeService, getFindItemsRequest(config, folder, offset), folder.getUser());
    }

    /**
     * Gets a list of ids for a specified folder by FindItemType for the current user.
     *
     * @param exchangeService the actual service to use when requesting ids
     * @param findItem the FindItemType to use for the request
     * @param targetUser The user to impersonate for the Exchange FindItem request.
     * @return a list of exchange ids
     * @throws ServiceCallException If we can't connect to the exchange service
     * @throws HttpErrorException If the response cannot be parsed
     */
    private static Vector<String> findItems(final ExchangeService exchangeService, final FindItemType findItem,
                                            final String targetUser)
            throws ServiceCallException, HttpErrorException
    {
        FindItemResponseType response = exchangeService.findItem(findItem, targetUser);

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
