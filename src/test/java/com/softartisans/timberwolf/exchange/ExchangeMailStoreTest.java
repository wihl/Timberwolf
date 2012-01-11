package com.softartisans.timberwolf.exchange;

import com.cloudera.alfredo.client.AuthenticationException;
import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfRealItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfFoldersType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.FindFolderParentType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import com.softartisans.timberwolf.MailboxItem;
import static com.softartisans.timberwolf.exchange.IsXmlBeansRequest.LikeThis;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Vector;
import org.apache.xmlbeans.XmlException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Test for ExchangeMailStore, uses mock exchange service */
public class ExchangeMailStoreTest extends ExchangeTestBase
{
    private final String idHeaderKey = "Item ID";

    @Test
    public void testGetGetItemsRequestNull()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(), GetItemHelper.getGetItemsRequest(null).xmlText());
    }

    @Test
    public void testGetGetItemsRequest0()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(), GetItemHelper.getGetItemsRequest(new ArrayList<String>()).xmlText());
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
        assertEquals(getItem.xmlText(), GetItemHelper.getGetItemsRequest(ids).xmlText());
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
        assertEquals(getItem.xmlText(), GetItemHelper.getGetItemsRequest(ids).xmlText());
    }

    @Test
    public void testGetItems0()
        throws ServiceCallException, HttpErrorException
    {
        Vector<String> list = new Vector<String>();
        Vector<MailboxItem> items = GetItemHelper.getItems(0, 0, list, service);
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
        mockGetItem(new MessageType[]{mockMessageItemId(idValue)}, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, 0, wholeList, service);
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
        mockGetItem(new MessageType[]{mockMessageItemId(idValue)}, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, 3, wholeList, service);
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
        mockGetItem(new MessageType[0], requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, 3, wholeList, service);
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
        mockGetItem(messages, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(91, 2, wholeList, service);
        assertEquals(requestedList.size(), items.size());
        for (int i = 0; i < requestedList.size(); i++)
        {
            assertEquals(requestedList.get(i), items.get(i).getHeader(idHeaderKey));
        }
    }

    private FolderType mockFolderType(String folderId)
    {
        FolderType folder = mock(FolderType.class);
        FolderIdType folderIdHolder = mock(FolderIdType.class);
        when(folder.isSetFolderId()).thenReturn(true);
        when(folder.getFolderId()).thenReturn(folderIdHolder);
        when(folderIdHolder.getId()).thenReturn(folderId);
        return folder;
    }


    private void mockGetItem(MessageType[] findResults, int initialOffset, int pageSize,
                             int pageIndex, int max, String folder)
            throws XmlException, ServiceCallException, IOException, HttpErrorException
    {
        int start = pageSize * pageIndex;
        max = Math.min(max, start + pageSize);
        mockGetItem(Arrays.copyOfRange(findResults, start, max),
                    generateIds(initialOffset + start, max - start, folder));
    }

    private void mockGetItem(MessageType[] messages, List<String> requestedList)
            throws XmlException, ServiceCallException, IOException, HttpErrorException
    {
        GetItemType getItem = GetItemHelper.getGetItemsRequest(requestedList);
        GetItemResponseType getItemResponse = mock(GetItemResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        ItemInfoResponseMessageType itemInfoResponseMessage = mock(ItemInfoResponseMessageType.class);
        ArrayOfRealItemsType arrayOfRealItems = mock(ArrayOfRealItemsType.class);
        when(service.getItem(LikeThis(getItem))).thenReturn(getItemResponse);
        when(getItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getGetItemResponseMessageArray())
                .thenReturn(new ItemInfoResponseMessageType[]{itemInfoResponseMessage});
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
        mockFindItem(messages);
        defaultMockFindFolders();
        for (MailboxItem mailboxItem : new ExchangeMailStore(service).getMail())
        {
            fail("There shouldn't be any mailBoxItems");
        }
    }

    @Test
    public void testGetMailFind0Folders()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException, AuthenticationException
    {
        // Exchange returns 0 mail when findItem is called
        mockFindFolders(new FolderType[0]);
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
        mockFindItem(messages);
        defaultMockFindFolders();

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
        mockFindItem(findItems);
        defaultMockFindFolders();
        mockGetItem(messages, requestedList);
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
            .thenReturn(new ItemInfoResponseMessageType[]{infoMessage});
        GetItemResponseType getResponse = mock(GetItemResponseType.class);
        when(getResponse.getResponseMessages()).thenReturn(responseArr);
        when(service.getItem(any(GetItemType.class))).thenReturn(getResponse);

        Vector<String> ids = new Vector<String>();
        ids.add("abcd");

        try
        {
            GetItemHelper.getItems(1, 0, ids, service);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response contained an error.", e.getMessage());
        }
    }

    @Test
    public void testFindMailOneIdPageTwoItemPages()
            throws IOException, AuthenticationException, ServiceCallException, HttpErrorException, XmlException
    {
        int itemsInExchange = 10;
        int idPageSize = 11;
        int itemPageSize = 5;
        defaultMockFindFolders();
        MessageType[] findResults = mockFindItem(defaultFolderId, 0, idPageSize, itemsInExchange);
        mockGetItem(findResults, 0, itemPageSize, 0, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 1, itemsInExchange, defaultFolderId);

        FindItemIterator mailItor = new FindItemIterator(service, defaultFolderId, idPageSize, itemPageSize);

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, defaultFolderId);
        while (mailItor.hasNext())
        {
            MailboxItem item = mailItor.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            index++;
        }
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testFindMailOneIdPageFiveItemPages()
            throws IOException, AuthenticationException, ServiceCallException, HttpErrorException, XmlException
    {
        int itemsInExchange = 24;
        int idPageSize = 30;
        int itemPageSize = 5;
        defaultMockFindFolders();
        MessageType[] findResults = mockFindItem(defaultFolderId, 0, idPageSize, itemsInExchange);
        mockGetItem(findResults, 0, itemPageSize, 0, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 1, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 2, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 3, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 4, itemsInExchange, defaultFolderId);

        FindItemIterator mailItor = new FindItemIterator(service, defaultFolderId, idPageSize, itemPageSize);

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, defaultFolderId);
        while (mailItor.hasNext())
        {
            MailboxItem item = mailItor.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            index++;
        }
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testFindMailTwoIdPages10ItemPages()
            throws IOException, AuthenticationException, ServiceCallException, HttpErrorException, XmlException
    {
        int itemsInExchange = 50;
        int idPageSize = 30;
        int itemPageSize = 5;
        defaultMockFindFolders();
        // FindItem #1
        MessageType[] findResults = mockFindItem(defaultFolderId, 0, idPageSize, idPageSize);
        mockGetItem(findResults, 0, itemPageSize, 0, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 1, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 2, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 3, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 4, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 5, itemsInExchange, defaultFolderId);
        // FindItem #2
        findResults = mockFindItem(defaultFolderId, idPageSize, idPageSize, itemsInExchange - idPageSize);
        mockGetItem(findResults, idPageSize, itemPageSize, 0, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, idPageSize, itemPageSize, 1, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, idPageSize, itemPageSize, 2, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, idPageSize, itemPageSize, 3, itemsInExchange, defaultFolderId);

        FindItemIterator mailItor = new FindItemIterator(service, defaultFolderId, idPageSize, itemPageSize);

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, defaultFolderId);
        while (mailItor.hasNext())
        {
            MailboxItem item = mailItor.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            index++;
        }
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testFindMailFiveIdPages20ItemPages()
            throws IOException, AuthenticationException, ServiceCallException, HttpErrorException, XmlException
    {
        int itemsInExchange = 100;
        int idPageSize = 20;
        int itemPageSize = 5;
        defaultMockFindFolders();
        for (int i = 0; i < 5; i++)
        {
            MessageType[] findResults = mockFindItem(defaultFolderId, i*idPageSize, idPageSize, idPageSize);
            for (int j = 0; j < 4; j++)
            {
                mockGetItem(findResults, idPageSize*i, itemPageSize, j, itemsInExchange, defaultFolderId);
            }
        }
        // because the idPageSize evenly divides the number of emails
        mockFindItem(defaultFolderId, itemsInExchange,idPageSize,0);

        FindItemIterator mailItor = new FindItemIterator(service, defaultFolderId, idPageSize, itemPageSize);

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, defaultFolderId);
        while (mailItor.hasNext())
        {
            MailboxItem item = mailItor.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            index++;
        }
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testFindMailItemPageLargerThanIdPage()
            throws IOException, AuthenticationException, ServiceCallException, HttpErrorException, XmlException
    {
        int itemsInExchange = 20;
        int idPageSize = 5;
        int itemPageSize = 10;
        defaultMockFindFolders();
        for (int i = 0; i < 4; i++)
        {
            MessageType[] findResults = mockFindItem(defaultFolderId, i*idPageSize, idPageSize, idPageSize);
            mockGetItem(findResults, idPageSize*i, idPageSize, 0, itemsInExchange, defaultFolderId);
        }
        // because the idPageSize evenly divides the number of emails
        mockFindItem(defaultFolderId, itemsInExchange,idPageSize,0);

        FindItemIterator mailItor = new FindItemIterator(service, defaultFolderId, idPageSize, itemPageSize);

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, defaultFolderId);
        while (mailItor.hasNext())
        {
            MailboxItem item = mailItor.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            index++;
        }
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testGetFindFoldersRequestDistinguished()
    {
        FindFolderType findFolder = FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.INBOX);
        assertEquals("IdOnly", findFolder.getFolderShape().getBaseShape().toString());
        assertTrue(findFolder.getParentFolderIds().getDistinguishedFolderIdArray().length == 1);
        assertEquals(DistinguishedFolderIdNameType.INBOX,
                     findFolder.getParentFolderIds().getDistinguishedFolderIdArray()[0].getId());
    }

    @Test
    public void testGetFindFoldersRequest()
    {
        String folderId = "Totally Not A Legit Folder Id";

        FindFolderType findFolder = FindFolderHelper.getFindFoldersRequest(folderId);
        assertEquals("IdOnly", findFolder.getFolderShape().getBaseShape().toString());
        assertTrue(findFolder.getParentFolderIds().getFolderIdArray().length == 1);
        assertEquals(folderId, findFolder.getParentFolderIds().getFolderIdArray()[0].getId());
    }

    @Test
    public void testFindFolders() throws ServiceCallException, HttpErrorException
    {
        int count = 10;

        List<String> ids = new ArrayList<String>(count);
        FolderType[] folders = new FolderType[count];

        for( int i = 0; i < count; i++)
        {
            String id = "SADG345GFGFEFHGGFH454fgH56FDDGFNGGERTTGH%$466" + i;
            ids.add(id);
            folders[i] = mockFolderType(id);
        }

        mockFindFolders(folders);

        FindFolderType findFoldersRequest = FindFolderHelper.getFindFoldersRequest(
                DistinguishedFolderIdNameType.MSGFOLDERROOT);
        Queue<String> foldersVec = FindFolderHelper.findFolders(service, findFoldersRequest);
        int folderCount = 0;
        for (String folder : foldersVec)
        {
            assertEquals(ids.get(folderCount), folder);
            folderCount++;
        }
    }

    @Test
    public void testFindFoldersNoRootFolder() throws ServiceCallException, HttpErrorException
    {
        FindFolderResponseType findFolderResponse = mock(FindFolderResponseType.class);
        ArrayOfResponseMessagesType findFolderArrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        FindFolderResponseMessageType findFolderResponseMessage = mock(FindFolderResponseMessageType.class);
        FindFolderType findFolder =
                FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT);
        when(service.findFolder(LikeThis(findFolder))).thenReturn(findFolderResponse);
        when(findFolderResponse.getResponseMessages()).thenReturn(findFolderArrayOfResponseMessages);
        when(findFolderArrayOfResponseMessages.getFindFolderResponseMessageArray())
                .thenReturn(new FindFolderResponseMessageType[]{findFolderResponseMessage});
        when(findFolderResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findFolderResponseMessage.isSetRootFolder()).thenReturn(false);
        FindFolderHelper.findFolders(service, findFolder);
    }

    @Test
    public void testFindFoldersNoFolders() throws ServiceCallException, HttpErrorException
    {
        FindFolderResponseType findFolderResponse = mock(FindFolderResponseType.class);
        ArrayOfResponseMessagesType findFolderArrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        FindFolderResponseMessageType findFolderResponseMessage = mock(FindFolderResponseMessageType.class);
        FindFolderParentType findFolderParent = mock(FindFolderParentType.class);
        FindFolderType findFolder =
                FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT);
        when(service.findFolder(LikeThis(findFolder))).thenReturn(findFolderResponse);
        when(findFolderResponse.getResponseMessages()).thenReturn(findFolderArrayOfResponseMessages);
        when(findFolderArrayOfResponseMessages.getFindFolderResponseMessageArray())
                .thenReturn(new FindFolderResponseMessageType[]{findFolderResponseMessage});
        when(findFolderResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findFolderResponseMessage.isSetRootFolder()).thenReturn(true);
        when(findFolderResponseMessage.getRootFolder()).thenReturn(findFolderParent);
        when(findFolderParent.isSetFolders()).thenReturn(false);
        FindFolderHelper.findFolders(service, findFolder);
    }

    @Test
    public void testFindFoldersNoFolderId() throws ServiceCallException, HttpErrorException
    {
        int count = 3;
        int unset = 1;
        FolderType[] messages = new FolderType[count];
        for (int i = 0; i < count; i++)
        {
            messages[i] = mockFolderType("the" + i + "id");
        }

        messages[unset] = mock(FolderType.class);
        when(messages[unset].isSetFolderId()).thenReturn(false);
        mockFindFolders(messages);
        Queue<String> items = FindFolderHelper.findFolders(
                service, FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT));
        Vector<String> expected = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            expected.add("the" + i + "id");
        }
        expected.remove(1);
        assertEquals(expected, items);
    }

    @Test
    public void testGetMailWithPagingAndFolders() throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        FindFolderResponseType folderResponse = mock(FindFolderResponseType.class);
        ArrayOfResponseMessagesType folderArr = mock(ArrayOfResponseMessagesType.class);
        FindFolderResponseMessageType folderMsgs = mock(FindFolderResponseMessageType.class);
        when(folderMsgs.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        FindFolderParentType parent = mock(FindFolderParentType.class);
        when(parent.isSetFolders()).thenReturn(true);
        ArrayOfFoldersType folders = mock(ArrayOfFoldersType.class);

        FolderType folderOne = mock(FolderType.class);
        FolderIdType folderOneId = mock(FolderIdType.class);
        when(folderOne.isSetFolderId()).thenReturn(true);
        when(folderOneId.getId()).thenReturn("FOLDER-ONE-ID");
        when(folderOne.getFolderId()).thenReturn(folderOneId);

        FolderType folderTwo = mock(FolderType.class);
        FolderIdType folderTwoId = mock(FolderIdType.class);
        when(folderTwo.isSetFolderId()).thenReturn(true);
        when(folderTwoId.getId()).thenReturn("FOLDER-TWO-ID");
        when(folderTwo.getFolderId()).thenReturn(folderTwoId);

        FolderType folderThree = mock(FolderType.class);
        FolderIdType folderThreeId = mock(FolderIdType.class);
        when(folderThree.isSetFolderId()).thenReturn(true);
        when(folderThreeId.getId()).thenReturn("FOLDER-THREE-ID");
        when(folderThree.getFolderId()).thenReturn(folderThreeId);

        when(folders.getFolderArray()).thenReturn(new FolderType[] { folderOne, folderTwo, folderThree });
        when(parent.getFolders()).thenReturn(folders);
        when(folderMsgs.getRootFolder()).thenReturn(parent);
        when(folderMsgs.isSetRootFolder()).thenReturn(true);
        FindFolderResponseMessageType[] fFRMT = new FindFolderResponseMessageType[] { folderMsgs };
        when(folderArr.getFindFolderResponseMessageArray()).thenReturn(fFRMT);
        when(folderResponse.getResponseMessages()).thenReturn(folderArr);

        FindFolderResponseType emptyFolderResponse = mock(FindFolderResponseType.class);
        ArrayOfResponseMessagesType emptyResponseArr = mock(ArrayOfResponseMessagesType.class);
        when(emptyResponseArr.getFindFolderResponseMessageArray()).thenReturn(new FindFolderResponseMessageType[] { });
        when(emptyFolderResponse.getResponseMessages()).thenReturn(emptyResponseArr);

        when(service.findFolder(LikeThis(FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT))))
            .thenReturn(folderResponse);

        mockFindItem("FOLDER-ONE-ID", 0, 10, 2);
        mockGetItem(new MessageType[] { mockMessageItemId("FOLDER-ONE-ID:the #0 id"),
                                        mockMessageItemId("FOLDER-ONE-ID:the #1 id") },
                    generateIds(0, 2, "FOLDER-ONE-ID"));
        mockFindItem("FOLDER-TWO-ID", 0, 10, 10);
        mockGetItem(new MessageType[] { mockMessageItemId("FOLDER-TWO-ID:the #0 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #1 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #2 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #3 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #4 id") },
                    generateIds(0, 5, "FOLDER-TWO-ID"));
        mockGetItem(new MessageType[] { mockMessageItemId("FOLDER-TWO-ID:the #5 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #6 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #7 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #8 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #9 id"), },
                    generateIds(5, 5, "FOLDER-TWO-ID"));
        mockFindItem("FOLDER-TWO-ID", 10, 10, 3);
        mockGetItem(new MessageType[] { mockMessageItemId("FOLDER-TWO-ID:the #10 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #11 id"),
                                        mockMessageItemId("FOLDER-TWO-ID:the #12 id"), },
                    generateIds(10, 3, "FOLDER-TWO-ID"));
        mockFindItem("FOLDER-THREE-ID", 0, 10, 2);
        mockGetItem(new MessageType[] { mockMessageItemId("FOLDER-THREE-ID:the #0 id"),
                                        mockMessageItemId("FOLDER-THREE-ID:the #1 id") },
                    generateIds(0, 2, "FOLDER-THREE-ID"));

        ExchangeMailStore store = new ExchangeMailStore(service, 10, 5);
        Iterator<MailboxItem> mail = store.getMail().iterator();
        for (String folder : new String[] { "FOLDER-ONE-ID", "FOLDER-TWO-ID", "FOLDER-THREE-ID" })
        {
            for (int i = 0; i < (folder == "FOLDER-TWO-ID" ? 13 : 2); i++)
            {
                assertTrue(mail.hasNext());
                MailboxItem item = mail.next();
                assertEquals(folder + ":the #" + i + " id", item.getHeader("Item ID"));
            }
        }
        assertFalse(mail.hasNext());
    }
}
