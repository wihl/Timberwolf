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

import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import com.ripariandata.timberwolf.MailboxItem;
import java.util.List;
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains helper functions for GetItem requests.
 */
public final class GetItemHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(GetItemHelper.class);

    /**
     * Enforces not being able to create an instance.
     */
    private GetItemHelper()
    {

    }

    /**
     * Creates a GetItemType to request the info for the given ids.
     *
     * @param ids The ids to request
     * @return The GetItemType necessary to request the info for those ids
     */
    static GetItemType getGetItemsRequest(final List<String> ids)
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
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
     * @param count The number of items to get.
     * @param startIndex The index in ids of the first item to get
     * @param ids A list of ids to get
     * If <tt>startIndex + count > ids.size()</tt> then only <tt>ids.size() - startIndex</tt>
     * items will be returned
     * @param exchangeService The backend service used for contacting Exchange.
     * @param targetUser The user to impersonate for the Exchange GetItem request.
     * @return A list of mailbox items that correspond to the given ids.
     * @throws HttpErrorException If the HTTP response from Exchange has a non-200 status code.
     * @throws ServiceCallException If there was a non-HTTP error making the Exchange
     *                              request, or if the SOAP find item response has a message
     *                              with a response code other than "No Error".
     */
    static Vector<MailboxItem> getItems(final int count, final int startIndex, final Vector<String> ids,
                                        final ExchangeService exchangeService, final String targetUser)
            throws ServiceCallException, HttpErrorException
    {
        int max = Math.min(startIndex + count, ids.size());
        if (max <= startIndex)
        {
            return new Vector<MailboxItem>();
        }
        LOG.trace("Making request:\n" + getGetItemsRequest(ids.subList(startIndex, max)).toString());
        GetItemResponseType response = exchangeService.getItem(getGetItemsRequest(ids.subList(startIndex, max)),
                                                               targetUser);

        if (response == null)
        {
            LOG.debug("Exchange service returned null get item response.");
            throw new ServiceCallException(ServiceCallException.Reason.OTHER, "Null response from Exchange service.");
        }

        ItemInfoResponseMessageType[] array = response.getResponseMessages().getGetItemResponseMessageArray();
        Vector<MailboxItem> items = new Vector<MailboxItem>();
        for (ItemInfoResponseMessageType message : array)
        {
            ResponseCodeType.Enum errorCode = message.getResponseCode();
            if (errorCode != null && errorCode != ResponseCodeType.NO_ERROR)
            {
                LOG.debug(errorCode.toString());
                throw new ServiceCallException(errorCode, "SOAP response contained an error.");
            }

            for (MessageType item : message.getItems().getMessageArray())
            {
                items.add(new ExchangeEmail(item));
            }

        }
        return items;
    }
}
