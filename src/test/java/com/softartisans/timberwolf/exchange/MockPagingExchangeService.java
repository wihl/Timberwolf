package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.*;
import com.microsoft.schemas.exchange.services.x2006.types.*;

import java.util.HashMap;
import java.util.Vector;

import static org.mockito.Mockito.*;

public class MockPagingExchangeService extends ExchangeService
{
    private MessageType[] messages;
    HashMap<String, MessageType> messageHash = new HashMap<String, MessageType>();

    private FindFolderParentType rootFolder;
    private FolderType[] folders;

    public MockPagingExchangeService(final MessageType[] msgs,
                                     final FindFolderParentType rootFolder, final FolderType[] folders)
    {
        // Since we're overriding all the public methods, the super
        // constructor doesn't really matter here.
        super("https://fake.com/ews/exchange.asmx");

        this.rootFolder = rootFolder;
        this.folders = folders;

        messages = msgs;

        for (MessageType msg : messages)
        {
            messageHash.put(msg.getItemId().getId(), msg);
        }
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

        NonEmptyArrayOfBaseItemIdsType ids = getItem.getItemIds();
        for (ItemIdType idType : ids.getItemIdArray())
        {
            String id = idType.getId();
            MessageType newMsg = itemsArr.addNewMessage();
            newMsg.addNewItemId().setId(messageHash.get(id).getItemId().getId());
        }

        msgArr.setGetItemResponseMessageArray(new ItemInfoResponseMessageType[] { getMsg });
        return response;
    }

    @Override
    public FindFolderResponseType findFolder(final FindFolderType findFolder)
    {
        FindFolderResponseType response = FindFolderResponseType.Factory.newInstance();
        ArrayOfResponseMessagesType msgArr = response.addNewResponseMessages();
        FindFolderResponseMessageType msg = FindFolderResponseMessageType.Factory.newInstance();
        msg.setResponseCode(ResponseCodeType.NO_ERROR);
        ArrayOfFoldersType foldersArr = rootFolder.addNewFolders();
        foldersArr.setFolderArray(folders);
        msg.setRootFolder(rootFolder);
        msgArr.setFindFolderResponseMessageArray(new FindFolderResponseMessageType[] { msg });

        return response;
    }
}
