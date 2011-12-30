package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfRealItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexBasePointType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexedPageViewType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;

public class MockPagingExchangeService extends ExchangeService
{
    private MessageType[] messages;

    public MockPagingExchangeService(final MessageType[] msgs)
    {
        // Since we're overriding all the public methods, the super
        // constructor doesn't really matter here.
        super("https://fake.com/ews/exchange.asmx");
        messages = msgs;
    }

    @Override
    public FindItemResponseType findItem(final FindItemType findItem)
    {
        int start = findItem.getIndexedPageItemView().getOffset();
        int end = Math.min(start + findItem.getIndexedPageItemView().getMaxEntriesReturned() - 1, messages.length - 1);

        FindItemResponseType response = FindItemResponseType.Factory.newInstance();
        ArrayOfResponseMessagesType msgArr = response.addNewResponseMessages();
        FindItemResponseMessageType findMsg = FindItemResponseMessageType.Factory.newInstance();
        findMsg.setResponseCode(ResponseCodeType.NO_ERROR);
        ArrayOfRealItemsType itemsArr = findMsg.addNewRootFolder().addNewItems();

        for (int i = start; i <= end; i++)
        {
            MessageType newMsg = itemsArr.addNewMessage();
            newMsg.addNewItemId().setId(messages[i].getItemId().getId());
        }

        msgArr.setFindItemResponseMessageArray(new FindItemResponseMessageType[] { findMsg });
        return response;
    }

    @Override
    public GetItemResponseType getItem(final GetItemType getItem)
    {
        GetItemResponseType response = GetItemResponseType.Factory.newInstance();
        ArrayOfResponseMessagesType msgArr = response.addNewResponseMessages();
        ItemInfoResponseMessageType getMsg = ItemInfoResponseMessageType.Factory.newInstance();
        getMsg.setResponseCode(ResponseCodeType.NO_ERROR);
        ArrayOfRealItemsType itemsArr = getMsg.addNewItems();

        // This is very slow.  Deal.
        NonEmptyArrayOfBaseItemIdsType ids = getItem.getItemIds();
        for (ItemIdType idType : ids.getItemIdArray())
        {
            String id = idType.getId();
            for (MessageType msg : messages)
            {
                if (msg.getItemId().getId() == id)
                {
                    MessageType newMsg = itemsArr.addNewMessage();
                    newMsg.addNewItemId().setId(id);
                    break;
                }
            }
        }

        msgArr.setGetItemResponseMessageArray(new ItemInfoResponseMessageType[] { getMsg });
        return response;
    }
}
