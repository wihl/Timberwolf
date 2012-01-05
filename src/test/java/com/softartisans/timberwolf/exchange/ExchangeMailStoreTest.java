package com.softartisans.timberwolf.exchange;

import com.cloudera.alfredo.client.AuthenticationException;
import com.microsoft.schemas.exchange.services.x2006.messages.*;
import com.microsoft.schemas.exchange.services.x2006.types.*;
import com.softartisans.timberwolf.MailboxItem;
import org.apache.xmlbeans.XmlException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import static com.softartisans.timberwolf.exchange.IsXmlBeansRequest.LikeThis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/** Test for ExchangeMailStore, uses mock exchange service */
public class ExchangeMailStoreTest
{
    @Mock
    private FindItemResponseType findItemResponse;
    @Mock
    private ArrayOfResponseMessagesType arrayOfResponseMessages;
    @Mock
    private FindItemResponseMessageType findItemResponseMessage;
    @Mock
    private FindItemParentType findItemParent;
    @Mock
    private ArrayOfRealItemsType arrayOfRealItems;
    @Mock
    private GetItemResponseType getItemResponse;
    @Mock
    private ItemInfoResponseMessageType itemInfoResponseMessage;
    private final String idHeaderKey = "Item ID";

    @Mock
    private FindFolderResponseType findFolderResponse;
    @Mock
    private ArrayOfResponseMessagesType findFolderArrayOfResponseMessages;
    @Mock
    private FindFolderResponseMessageType findFolderResponseMessage;
    @Mock
    private FindFolderParentType findFolderParent;
    @Mock
    private ArrayOfFoldersType findFolderArrayOfFolders;
    @Mock
    private FolderType folderType;
    @Mock
    private FolderIdType folderIdType;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetFindItemsRequestInbox()
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        DistinguishedFolderIdType folderId = findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.INBOX);
        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        index.setMaxEntriesReturned(1000);
        index.setBasePoint(IndexBasePointType.BEGINNING);
        index.setOffset(0);
        assertEquals(findItem.xmlText(),
                ExchangeMailStore.getFindItemsRequest(DistinguishedFolderIdNameType.INBOX, 0, 1000).xmlText());
    }

    @Test
    public void testGetFindItemsRequestDeletedItems()
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        DistinguishedFolderIdType folderId = findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.DELETEDITEMS);
        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        index.setMaxEntriesReturned(1000);
        index.setBasePoint(IndexBasePointType.BEGINNING);
        index.setOffset(0);
        assertEquals(findItem.xmlText(),
                     ExchangeMailStore.getFindItemsRequest(DistinguishedFolderIdNameType.DELETEDITEMS, 0, 1000).xmlText());
    }

    @Test
    public void testFindItemsInboxRespondNull()
        throws ServiceCallException, HttpErrorException
    {
        ExchangeService service = mock(ExchangeService.class);
        FindItemType findItem = ExchangeMailStore.getFindItemsRequest(DistinguishedFolderIdNameType.INBOX, 0, 1000);
        when(service.findItem(LikeThis(findItem))).thenReturn(null);

        try
        {
            Vector<String> items = ExchangeMailStore.findItems(service, 0, 1000);
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
        ExchangeService service = mockFindItem(messages);
        Vector<String> items = ExchangeMailStore.findItems(service, 0, 1000);
        assertEquals(0, items.size());
    }

    @Test
    public void testFindItemsItemsRespond1()
        throws ServiceCallException, HttpErrorException
    {
        MessageType message = mockMessageItemId("foobar27");
        MessageType[] messages = new MessageType[]{message};
        ExchangeService service = mockFindItem(messages);
        Vector<String> items = ExchangeMailStore.findItems(service, 0, 1000);
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
        ExchangeService service = mockFindItem(messages);
        Vector<String> items = ExchangeMailStore.findItems(service, 0, 1000);
        Vector<String> expected = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            expected.add("the" + i + "id");
        }
        assertEquals(expected, items);
    }

    private ExchangeService mockFindItem(MessageType[] messages)
        throws ServiceCallException, HttpErrorException
    {
        ExchangeService service = mock(ExchangeService.class);
        FindItemType findItem = ExchangeMailStore.getFindItemsRequest(DistinguishedFolderIdNameType.INBOX, 0, 1000);
        when(service.findItem(LikeThis(findItem))).thenReturn(findItemResponse);
        when(findItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getFindItemResponseMessageArray())
            .thenReturn(new FindItemResponseMessageType[]{ findItemResponseMessage });
        when(findItemResponseMessage.getRootFolder()).thenReturn(findItemParent);
        // For logging right now, might actually be checked later
        when(findItemResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findItemParent.getItems()).thenReturn(arrayOfRealItems);
        when(arrayOfRealItems.getMessageArray()).thenReturn(messages);

        FindFolderType findFolder =
                ExchangeMailStore.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT);
        when(service.findFolder(LikeThis(findFolder))).thenReturn(findFolderResponse);
        when(findFolderResponse.getResponseMessages()).thenReturn(findFolderArrayOfResponseMessages);
        when(findFolderArrayOfResponseMessages.getFindFolderResponseMessageArray())
                .thenReturn(new FindFolderResponseMessageType[]{findFolderResponseMessage});
        when(findFolderResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findFolderResponseMessage.getRootFolder()).thenReturn(findFolderParent);
        when(findFolderParent.getFolders()).thenReturn(findFolderArrayOfFolders);
        when(findFolderArrayOfFolders.getFolderArray()).thenReturn(new FolderType[] {folderType});
        when(folderType.getFolderId()).thenReturn(folderIdType);
        when(folderIdType.getId()).thenReturn("ANAMAZINGLYENGLISH-LIKEGUID");
        return service;
    }

    @Test
    public void testGetGetItemsRequestNull()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(), ExchangeMailStore.getGetItemsRequest(null).xmlText());
    }

    @Test
    public void testGetGetItemsRequest0()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(), ExchangeMailStore.getGetItemsRequest(new ArrayList<String>()).xmlText());
    }

    @Test
    public void testGetGetItemsRequest1()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        items.addNewItemId().setId("idNumber0");
        ArrayList<String> ids = new ArrayList<String>();
        ids.add("idNumber0");
        assertEquals(getItem.xmlText(), ExchangeMailStore.getGetItemsRequest(ids).xmlText());
    }

    @Test
    public void testGetGetItemsRequest100()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        for (int i = 0; i < 100; i++)
        {
            items.addNewItemId().setId("idNumber" + i);
        }
        ArrayList<String> ids = new ArrayList<String>();
        for (int i = 0; i < 100; i++)
        {
            ids.add("idNumber" + i);
        }
        assertEquals(getItem.xmlText(), ExchangeMailStore.getGetItemsRequest(ids).xmlText());
    }

    @Test
    public void testGetItems0()
        throws ServiceCallException, HttpErrorException
    {
        ExchangeService service = mock(ExchangeService.class);
        Vector<String> list = new Vector<String>();
        Vector<MailboxItem> items = ExchangeMailStore.getItems(0, 0, list, service);
        assertEquals(0, items.size());
    }

    @Test
    public void testGetItems0to1()
        throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        Vector<String> wholeList = new Vector<String>(5);
        for (int i = 0; i < 5; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        wholeList.set(0, idValue);
        requestedList.add(idValue);
        ExchangeService service = mockGetItem(new MessageType[]{mockMessageItemId(idValue)}, requestedList);
        Vector<MailboxItem> items = ExchangeMailStore.getItems(1, 0, wholeList, service);
        assertEquals(1, items.size());
        assertEquals(idValue, items.get(0).getHeader(idHeaderKey));
    }

    @Test
    public void testGetItems3to4()
        throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        Vector<String> wholeList = new Vector<String>(5);
        for (int i = 0; i < 5; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        wholeList.set(3, idValue);
        requestedList.add(idValue);
        ExchangeService service = mockGetItem(new MessageType[]{mockMessageItemId(idValue)}, requestedList);
        Vector<MailboxItem> items = ExchangeMailStore.getItems(1, 3, wholeList, service);
        assertEquals(1, items.size());
        assertEquals(idValue, items.get(0).getHeader(idHeaderKey));
    }

    @Test
    public void testGetItems2to3Return0()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException, AuthenticationException
    {
        Vector<String> wholeList = new Vector<String>(5);
        for (int i = 0; i < 5; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        wholeList.set(3, idValue);
        requestedList.add(idValue);
        ExchangeService service = mockGetItem(new MessageType[0], requestedList);
        Vector<MailboxItem> items = ExchangeMailStore.getItems(1, 3, wholeList, service);
        assertEquals(0, items.size());
    }

    @Test
    public void testGetItems2to93()
        throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        Vector<String> wholeList = new Vector<String>(100);
        for (int i = 0; i < 100; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        MessageType[] messages = new MessageType[91];
        for (int i = 2; i < 93; i++)
        {
            String id = "id #" + i;
            wholeList.set(i, id);
            requestedList.add(id);
            messages[i - 2] = mockMessageItemId(id);
        }
        ExchangeService service = mockGetItem(messages, requestedList);
        Vector<MailboxItem> items = ExchangeMailStore.getItems(91, 2, wholeList, service);
        assertEquals(requestedList.size(), items.size());
        for (int i = 0; i < requestedList.size(); i++)
        {
            assertEquals(requestedList.get(i), items.get(i).getHeader(idHeaderKey));
        }
    }

    private MessageType mockMessageItemId(String itemId)
    {

        MessageType mockedMessage = mock(MessageType.class);
        ItemIdType mockedId = mock(ItemIdType.class);
        when(mockedMessage.isSetItemId()).thenReturn(true);
        when(mockedMessage.getItemId()).thenReturn(mockedId);
        when(mockedId.getId()).thenReturn(itemId);
        return mockedMessage;
    }

    private ExchangeService mockGetItem(MessageType[] messages, List<String> requestedList)
        throws ServiceCallException, HttpErrorException, HttpErrorException, XmlException, IOException
    {
        ExchangeService service = mock(ExchangeService.class);
        mockGetItem(messages, requestedList, service);
        return service;
    }

    private void mockGetItem(MessageType[] messages, List<String> requestedList,
                             ExchangeService service)
            throws XmlException, ServiceCallException, IOException, HttpErrorException
    {
        GetItemType getItem = ExchangeMailStore.getGetItemsRequest(requestedList);
        when(service.getItem(LikeThis(getItem))).thenReturn(getItemResponse);
        when(getItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getGetItemResponseMessageArray())
                .thenReturn(new ItemInfoResponseMessageType[]{ itemInfoResponseMessage });
        when(itemInfoResponseMessage.getItems()).thenReturn(arrayOfRealItems);
        when(arrayOfRealItems.getMessageArray()).thenReturn(messages);
    }

    @Test
    public void testGetMailFind0()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException, AuthenticationException
    {
        // Exchange returns 0 mail when findItem is called
        MessageType[] messages = new MessageType[0];
        ExchangeService service = mockFindItem(messages);
        for (MailboxItem mailboxItem : new ExchangeMailStore(service).getMail())
        {
            fail("There shouldn't be any mailBoxItems");
        }
    }

    @Test
    public void testGetMailGet0()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException, AuthenticationException
    {
        // Exchange returns 0 mail even though you asked for some mail
        int count = 100;
        MessageType[] messages = new MessageType[count];
        for (int i = 0; i < count; i++)
        {
            messages[i] = mockMessageItemId("the" + i + "id");
        }
        ExchangeService service = mockFindItem(messages);

        try
        {
            Iterable<MailboxItem> mail = new ExchangeMailStore(service).getMail();
        }
        catch (ExchangeRuntimeException e)
        {
            assertEquals("Failed to get item details.", e.getMessage());
        }
    }

    @Test
    public void testGetMail30()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException, AuthenticationException
    {
        // Exchange returns 30 in FindItems and 30 in GetItems
        int count = 30;
        MessageType[] findItems = new MessageType[count];
        List<String> requestedList = new Vector<String>(count);
        MessageType[] messages = new MessageType[count];
        for (int i = 0; i < 30; i++)
        {
            String id = "the #" + i + " id";
            findItems[i] = mockMessageItemId(id);
            requestedList.add(id);
            messages[i] = mockMessageItemId(id);
        }
        ExchangeService service = mockFindItem(findItems);
        mockGetItem(messages, requestedList, service);
        int i = 0;
        for (MailboxItem mailboxItem : new ExchangeMailStore(service).getMail())
        {
            assertEquals(requestedList.get(i), mailboxItem.getHeader(idHeaderKey));
            i++;
        }
        if (i < requestedList.size())
        {
            fail("There were less items returned than there should have been");
        }
    }

    @Test
    public void testGetItemsWithErrorResponse()
        throws ServiceCallException, HttpErrorException
    {
        ItemInfoResponseMessageType infoMessage = mock(ItemInfoResponseMessageType.class);
        when(infoMessage.getResponseCode()).thenReturn(ResponseCodeType.ERROR_ACCESS_DENIED);
        ArrayOfResponseMessagesType responseArr = mock(ArrayOfResponseMessagesType.class);
        when(responseArr.getGetItemResponseMessageArray())
            .thenReturn(new ItemInfoResponseMessageType[] { infoMessage });
        GetItemResponseType getResponse = mock(GetItemResponseType.class);
        when(getResponse.getResponseMessages()).thenReturn(responseArr);
        ExchangeService service = mock(ExchangeService.class);
        when(service.getItem(any(GetItemType.class))).thenReturn(getResponse);

        ExchangeMailStore mailstore = new ExchangeMailStore(service);
        Vector<String> ids = new Vector<String>();
        ids.add("abcd");

        try
        {
            mailstore.getItems(1, 0, ids, service);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response contained an error.", e.getMessage());
        }
    }

    @Test
    public void testFindItemsWithErrorResponse()
        throws ServiceCallException, HttpErrorException
    {
        FindItemResponseMessageType findMessage = mock(FindItemResponseMessageType.class);
        when(findMessage.getResponseCode()).thenReturn(ResponseCodeType.ERROR_ACCESS_DENIED);
        ArrayOfResponseMessagesType responseArr = mock(ArrayOfResponseMessagesType.class);
        when(responseArr.getFindItemResponseMessageArray())
            .thenReturn(new FindItemResponseMessageType[] { findMessage });
        FindItemResponseType findResponse = mock(FindItemResponseType.class);
        when(findResponse.getResponseMessages()).thenReturn(responseArr);
        ExchangeService service = mock(ExchangeService.class);
        when(service.findItem(any(FindItemType.class))).thenReturn(findResponse);

        ExchangeMailStore mailstore = new ExchangeMailStore(service);

        try
        {
            mailstore.findItems(service, 0, 1000);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response contained an error.", e.getMessage());
        }
    }

    @Test
    public void testGetFindItemsRequestOffset()
    {
        ExchangeService service = mock(ExchangeService.class);
        ExchangeMailStore mailstore = new ExchangeMailStore(service);
        DistinguishedFolderIdNameType.Enum folder = DistinguishedFolderIdNameType.INBOX;

        FindItemType request = mailstore.getFindItemsRequest(folder, 3, 10);
        assertEquals(3, request.getIndexedPageItemView().getOffset());

        request = mailstore.getFindItemsRequest(folder, 13, 10);
        assertEquals(13, request.getIndexedPageItemView().getOffset());

        request = mailstore.getFindItemsRequest(folder, 0, 10);
        assertEquals(0, request.getIndexedPageItemView().getOffset());

        request = mailstore.getFindItemsRequest(folder, -1, 10);
        assertEquals(0, request.getIndexedPageItemView().getOffset());
    }

    @Test
    public void testGetFindItemsRequestMaxEntries()
    {
        ExchangeService service = mock(ExchangeService.class);
        ExchangeMailStore mailstore = new ExchangeMailStore(service);
        DistinguishedFolderIdNameType.Enum folder = DistinguishedFolderIdNameType.INBOX;

        FindItemType request = mailstore.getFindItemsRequest(folder, 5, 10);
        assertEquals(10, request.getIndexedPageItemView().getMaxEntriesReturned());

        request = mailstore.getFindItemsRequest(folder, 5, 3);
        assertEquals(3, request.getIndexedPageItemView().getMaxEntriesReturned());

        request = mailstore.getFindItemsRequest(folder, 5, 0);
        assertEquals(1, request.getIndexedPageItemView().getMaxEntriesReturned());
    }

    private void assertPagesThroughItems(int itemCount, int idPageSize, int itemPageSize) throws IOException, AuthenticationException
    {
        MessageType[] messages = new MessageType[itemCount];
        for (int i = 0; i < itemCount; i++)
        {
            MessageType mockedMessage = mock(MessageType.class);
            ItemIdType mockedId = mock(ItemIdType.class);
            when(mockedMessage.isSetItemId()).thenReturn(true);
            when(mockedMessage.getItemId()).thenReturn(mockedId);
            when(mockedId.getId()).thenReturn("item " + i);
            messages[i] = mockedMessage;
        }

        ExchangeService service = new MockPagingExchangeService(messages);
        ExchangeMailStore.EmailIterator mailItor =
            new ExchangeMailStore.EmailIterator(service, idPageSize, itemPageSize);

        int count = 0;
        while (mailItor.hasNext())
        {
            MailboxItem item = mailItor.next();
            assertEquals("item " + count, item.getHeader("Item ID"));
            count++;
        }
        assertEquals(itemCount, count);
    }

    @Test
    public void testFindMailOneIdPageTwoItemPages() throws IOException, AuthenticationException
    {
        assertPagesThroughItems(10, 11, 5);
    }

    @Test
    public void testFindMailOneIdPageFiveItemPages() throws IOException, AuthenticationException
    {
        assertPagesThroughItems(24, 30, 5);
    }

    @Test
    public void testFindMailTwoIdPages10ItemPages() throws IOException, AuthenticationException
    {
        assertPagesThroughItems(50, 30, 5);
    }

    @Test
    public void testFindMailFiveIdPages20ItemPages() throws IOException, AuthenticationException
    {
        assertPagesThroughItems(100, 20, 5);
    }

    @Test
    public void testGetFindFoldersRequestDistinguished()
    {
        ExchangeService service = mock(ExchangeService.class);
        ExchangeMailStore mail = new ExchangeMailStore(service);

        FindFolderType findFolder = mail.getFindFoldersRequest(DistinguishedFolderIdNameType.INBOX);
        assertEquals("AllProperties", findFolder.getFolderShape().getBaseShape().toString());
        assertTrue(findFolder.getParentFolderIds().getDistinguishedFolderIdArray().length == 1);
        assertEquals(DistinguishedFolderIdNameType.INBOX,
                findFolder.getParentFolderIds().getDistinguishedFolderIdArray()[0].getId());
    }

    @Test
    public void testGetFindFoldersRequest()
    {
        ExchangeService service = mock(ExchangeService.class);
        ExchangeMailStore mail = new ExchangeMailStore(service);
        String folderId = "Totally Not A Legit Folder Id";

        FindFolderType findFolder = mail.getFindFoldersRequest(folderId);
        assertEquals("AllProperties", findFolder.getFolderShape().getBaseShape().toString());
        assertTrue(findFolder.getParentFolderIds().getFolderIdArray().length == 1);
        assertEquals(folderId, findFolder.getParentFolderIds().getFolderIdArray()[0].getId());
    }

    @Test
    public void testFindFolders()
    {
        FindFolderParentType rootFolder = FindFolderParentType.Factory.newInstance();
        int count = 10;

        FolderType[] folders = new FolderType[count];

        for( int i = 0; i < count; i++)
        {
            FolderType folder = FolderType.Factory.newInstance();
            folder.setDisplayName("Folder Number " + i);
            FolderIdType folderId = FolderIdType.Factory.newInstance();
            folderId.setId("SADG345GFGFEFHGGFH454fgH56FDDGFNGGERTTGH%$466" + i);
            folderId.setChangeKey("HHYtryyry==" + i);
            folder.setFolderId(folderId);
            folders[i] = folder;
        }
        MockPagingExchangeService service = new MockPagingExchangeService(rootFolder, folders);

        try
        {
            Queue<String> foldersVec = ExchangeMailStore.findFolders(service, "TotallyUnimportantId");
            int folderCount = 0;
            for( String folder : foldersVec)
            {
                assertEquals(folders[folderCount].getFolderId().getId(), folder);
                folderCount++;
            }
        }
        catch(Exception e)
        {
            Assert.fail(e.getMessage());
        }
    }
}
