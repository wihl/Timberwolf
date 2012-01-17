package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexBasePointType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexedPageViewType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;

import java.util.Vector;

import org.junit.Test;

import static com.softartisans.timberwolf.exchange.IsXmlBeansRequest.LikeThis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for all the FindItem specific stuff.
 */
public class FindItemTest extends ExchangeTestBase
{
    private FolderContext inbox =
            new FolderContext(DistinguishedFolderIdNameType.INBOX);

    private static int DEFAULT_MAX_FIND_ITEMS = 1000;
    @Test
    public void testGetFindItemsRequestInbox()
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId = findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.INBOX);
        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        index.setMaxEntriesReturned(DEFAULT_MAX_FIND_ITEMS);
        index.setBasePoint(IndexBasePointType.BEGINNING);
        index.setOffset(0);
        Configuration config = new Configuration(DEFAULT_MAX_FIND_ITEMS, 0);
        assertEquals(findItem.xmlText(),
                     FindItemHelper.getFindItemsRequest(config, inbox, 0).xmlText());
    }

    @Test
    public void testGetFindItemsRequestDeletedItems()
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId = findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.DELETEDITEMS);
        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        index.setMaxEntriesReturned(DEFAULT_MAX_FIND_ITEMS);
        index.setBasePoint(IndexBasePointType.BEGINNING);
        index.setOffset(0);
        Configuration config = new Configuration(DEFAULT_MAX_FIND_ITEMS, 0);
        FolderContext folder = new FolderContext(DistinguishedFolderIdNameType.DELETEDITEMS);
        assertEquals(findItem.xmlText(),
                     FindItemHelper.getFindItemsRequest(config, folder, 0).xmlText());
    }

    @Test
    public void testGetFindItemsRequestOffset()
    {
        int count = 10;
        Configuration config = new Configuration(count, 0);

        int assertCount = 3;
        FindItemType request = FindItemHelper.getFindItemsRequest(config, inbox, assertCount);
        assertEquals(assertCount, request.getIndexedPageItemView().getOffset());

        assertCount = 13;
        request = FindItemHelper.getFindItemsRequest(config, inbox, assertCount);
        assertEquals(assertCount, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, inbox, 0);
        assertEquals(0, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, inbox, -1);
        assertEquals(0, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, inbox, 1);
        assertEquals(1, request.getIndexedPageItemView().getOffset());
    }

    private void assertFindItemsRequestMaxEntries(final int maxItems)
    {
        Configuration config = new Configuration(maxItems, 0);
        int count = 5;
        FindItemType request = FindItemHelper.getFindItemsRequest(config, inbox, count);
        assertEquals(Math.max(1, maxItems), request.getIndexedPageItemView().getMaxEntriesReturned());
    }

    @Test
    public void testGetFindItemsRequestMaxEntries()
    {
        int arbitraryValues[] = { 10, 3, 0, 1};
        assertFindItemsRequestMaxEntries(arbitraryValues[0]);
        assertFindItemsRequestMaxEntries(arbitraryValues[1]);
        assertFindItemsRequestMaxEntries(arbitraryValues[2]);
        assertFindItemsRequestMaxEntries(arbitraryValues[3]);
    }

    @Test
    public void testFindItemsInboxRespondNull()
            throws ServiceCallException, HttpErrorException
    {
        Configuration config = new Configuration(DEFAULT_MAX_FIND_ITEMS, 0);
        FindItemType findItem = FindItemHelper.getFindItemsRequest(config, inbox, 0);
        when(service.findItem(LikeThis(findItem))).thenReturn(null);

        try
        {
            Vector<String> items = FindItemHelper.findItems(service, config, inbox, 0);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("Null response from Exchange service.", e.getMessage());
        }
    }

    @Test
    public void testFindItemsItemsRespond0()
            throws ServiceCallException, HttpErrorException
    {
        MessageType[] messages = new MessageType[0];
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(service, defaultConfig, defaultFolder, 0);
        assertEquals(0, items.size());
    }

    @Test
    public void testFindItemsItemsRespond1()
            throws ServiceCallException, HttpErrorException
    {
        MessageType message = mockMessageItemId("foobar27");
        MessageType[] messages = new MessageType[]{message};
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(service, defaultConfig, defaultFolder, 0);
        Vector<String> expected = new Vector<String>(1);
        expected.add("foobar27");
        assertEquals(expected, items);
    }

    @Test
    public void testFindItemsItemsRespond100()
            throws ServiceCallException, HttpErrorException
    {
        int count = 100;
        MessageType[] messages = new MessageType[count];
        for (int i = 0; i < count; i++)
        {
            messages[i] = mockMessageItemId("the" + i + "id");
        }
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(service, defaultConfig, defaultFolder, 0);
        Vector<String> expected = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            expected.add("the" + i + "id");
        }
        assertEquals(expected, items);
    }

    @Test
    public void testFindItemsItemsMissingId()
            throws ServiceCallException, HttpErrorException
    {
        int count = 3;
        int unset = 1;
        MessageType[] messages = new MessageType[count];
        for (int i = 0; i < count; i++)
        {
            messages[i] = mockMessageItemId("the" + i + "id");
        }

        messages[unset] = mock(MessageType.class);
        when(messages[unset].isSetItemId()).thenReturn(false);
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(service, defaultConfig, defaultFolder, 0);
        Vector<String> expected = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            expected.add("the" + i + "id");
        }
        expected.remove(1);
        assertEquals(expected, items);
    }

    @Test
    public void testFindItemsWithErrorResponse()
            throws ServiceCallException, HttpErrorException
    {
        FindItemResponseMessageType findMessage = mock(FindItemResponseMessageType.class);
        when(findMessage.getResponseCode()).thenReturn(ResponseCodeType.ERROR_ACCESS_DENIED);
        ArrayOfResponseMessagesType responseArr = mock(ArrayOfResponseMessagesType.class);
        when(responseArr.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{findMessage});
        FindItemResponseType findResponse = mock(FindItemResponseType.class);
        when(findResponse.getResponseMessages()).thenReturn(responseArr);
        when(service.findItem(any(FindItemType.class))).thenReturn(findResponse);

        try
        {
            Configuration config = new Configuration(DEFAULT_MAX_FIND_ITEMS, 0);
            FindItemHelper.findItems(service, config, inbox, 0);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response contained an error.", e.getMessage());
        }
    }

    @Test
    public void testFindItemsNoRootFolder() throws ServiceCallException, HttpErrorException
    {
        FindItemResponseType findItemResponse = mock(FindItemResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        FindItemResponseMessageType findItemResponseMessage = mock(FindItemResponseMessageType.class);
        FindItemParentType findItemParent = mock(FindItemParentType.class);
        FindItemType findItem = FindItemHelper.getFindItemsRequest(defaultConfig, defaultFolder, 0);
        when(service.findItem(LikeThis(findItem))).thenReturn(findItemResponse);
        when(findItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{findItemResponseMessage});
        when(findItemResponseMessage.getRootFolder()).thenReturn(findItemParent);
        when(findItemResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findItemResponseMessage.isSetRootFolder()).thenReturn(false);
    }

    @Test
    public void testFindItemsNoId() throws ServiceCallException, HttpErrorException
    {
        FindItemResponseType findItemResponse = mock(FindItemResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        FindItemResponseMessageType findItemResponseMessage = mock(FindItemResponseMessageType.class);
        FindItemParentType findItemParent = mock(FindItemParentType.class);
        FindItemType findItem = FindItemHelper.getFindItemsRequest(defaultConfig, defaultFolder, 0);
        when(service.findItem(LikeThis(findItem))).thenReturn(findItemResponse);
        when(findItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{ findItemResponseMessage });
        when(findItemResponseMessage.getRootFolder()).thenReturn(findItemParent);
        when(findItemResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findItemResponseMessage.isSetRootFolder()).thenReturn(false);
    }

}
