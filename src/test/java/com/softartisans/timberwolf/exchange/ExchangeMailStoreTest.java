package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfRealItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfFoldersType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.FindFolderParentType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.softartisans.timberwolf.MailboxItem;
import com.softartisans.timberwolf.NoopUserTimeUpdater;
import com.softartisans.timberwolf.UserTimeUpdater;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static com.softartisans.timberwolf.exchange.IsXmlBeansRequest.LikeThis;
import org.junit.Before;
import org.junit.Test;

/** Test for ExchangeMailStore, uses mock exchange service */
public class ExchangeMailStoreTest extends ExchangeTestBase
{
    private final String idHeaderKey = "Item ID";
    ArrayList<String> defaultUsers;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        defaultUsers = new ArrayList<String>();
        defaultUsers.add("bkerr");
    }

    @Test
    public void testGetMailFind0()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException
    {
        // Exchange returns 0 mail when findItem is called
        MessageType[] messages = new MessageType[0];
        mockFindItem(messages);
        defaultMockFindFolders();
        for (MailboxItem mailboxItem : new ExchangeMailStore(service).getMail(defaultUsers, new NoopUserTimeUpdater()))
        {
            fail("There shouldn't be any mailBoxItems");
        }
    }

    @Test
    public void testGetMailFind0Folders()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException
    {
        // Exchange returns 0 mail when findItem is called
        mockFindFolders(new FolderType[0]);
        for (MailboxItem mailboxItem : new ExchangeMailStore(service).getMail(defaultUsers, new NoopUserTimeUpdater()))
        {
            fail("There shouldn't be any mailBoxItems");
        }
    }

    @Test
    public void testGetMailGet0()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException
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
            Iterable<MailboxItem> mail = new ExchangeMailStore(service).getMail(defaultUsers, new NoopUserTimeUpdater());
        }
        catch (ExchangeRuntimeException e)
        {
            assertEquals("Failed to get item details.", e.getMessage());
        }
    }

    @Test
    public void testGetMail30()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException
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
        for (MailboxItem mailboxItem : new ExchangeMailStore(service).getMail(defaultUsers, new NoopUserTimeUpdater()))
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
    public void testFindMailOneIdPageTwoItemPages()
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        int itemsInExchange = 10;
        int idPageSize = 11;
        int itemPageSize = 5;
        Configuration config = new Configuration(idPageSize, itemPageSize);
        defaultMockFindFolders();
        MessageType[] findResults = mockFindItem(defaultFolderId, 0, idPageSize, itemsInExchange);
        mockGetItem(findResults, 0, itemPageSize, 0, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 1, itemsInExchange, defaultFolderId);

        FindItemIterator mailItor = new FindItemIterator(service, config, defaultFolder);

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
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        int itemsInExchange = 24;
        int idPageSize = 30;
        int itemPageSize = 5;
        Configuration config = new Configuration(idPageSize, itemPageSize);
        defaultMockFindFolders();
        MessageType[] findResults = mockFindItem(defaultFolderId, 0, idPageSize, itemsInExchange);
        mockGetItem(findResults, 0, itemPageSize, 0, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 1, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 2, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 3, itemsInExchange, defaultFolderId);
        mockGetItem(findResults, 0, itemPageSize, 4, itemsInExchange, defaultFolderId);

        FindItemIterator mailItor = new FindItemIterator(service, config, defaultFolder);

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
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        int itemsInExchange = 50;
        int idPageSize = 30;
        int itemPageSize = 5;
        Configuration config = new Configuration(idPageSize, itemPageSize);
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

        FindItemIterator mailItor = new FindItemIterator(service, config, defaultFolder);

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
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        int itemsInExchange = 100;
        int idPageSize = 20;
        int itemPageSize = 5;
        Configuration config = new Configuration(idPageSize, itemPageSize);
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

        FindItemIterator mailItor = new FindItemIterator(service, config, defaultFolder);

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
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        int itemsInExchange = 20;
        int idPageSize = 5;
        int itemPageSize = 10;
        Configuration config = new Configuration(idPageSize, itemPageSize);
        defaultMockFindFolders();
        for (int i = 0; i < 4; i++)
        {
            MessageType[] findResults = mockFindItem(defaultFolderId, i*idPageSize, idPageSize, idPageSize);
            mockGetItem(findResults, idPageSize*i, idPageSize, 0, itemsInExchange, defaultFolderId);
        }
        // because the idPageSize evenly divides the number of emails
        mockFindItem(defaultFolderId, itemsInExchange,idPageSize,0);

        FindItemIterator mailItor = new FindItemIterator(service, config, defaultFolder);

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

        when(service.findFolder(LikeThis(FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT)), eq(defaultUser)))
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
        Iterator<MailboxItem> mail = store.getMail(defaultUsers, new NoopUserTimeUpdater()).iterator();
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

    @Test
    public void testGetMailMultipleUsers() throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        FolderType aliceFolder = mock(FolderType.class);
        FolderIdType aliceId = mock(FolderIdType.class);
        when(aliceFolder.isSetFolderId()).thenReturn(true);
        when(aliceFolder.getFolderId()).thenReturn(aliceId);
        when(aliceId.getId()).thenReturn("ALICE-FOLDER");

        FolderType bobFolder = mock(FolderType.class);
        FolderIdType bobId = mock(FolderIdType.class);
        when(bobFolder.isSetFolderId()).thenReturn(true);
        when(bobFolder.getFolderId()).thenReturn(bobId);
        when(bobId.getId()).thenReturn("BOB-FOLDER");

        mockFindFolders(new FolderType[] { aliceFolder }, "alice");
        mockFindFolders(new FolderType[] { bobFolder }, "bob");
        mockFindItem("ALICE-FOLDER", 0, 10, 2, "alice");
        mockGetItem(new MessageType[] { mockMessageItemId("ALICE-FOLDER:the #0 id"),
                                        mockMessageItemId("ALICE-FOLDER:the #1 id") },
                    generateIds(0, 2, "ALICE-FOLDER"), "alice");
        mockFindItem("BOB-FOLDER", 0, 10, 2, "bob");
        mockGetItem(new MessageType[] { mockMessageItemId("BOB-FOLDER:the #0 id"),
                                        mockMessageItemId("BOB-FOLDER:the #1 id") },
                    generateIds(0, 2, "BOB-FOLDER"), "bob");

        ArrayList<String> users = new ArrayList<String>();
        users.add("bob");
        users.add("alice");

        ExchangeMailStore store = new ExchangeMailStore(service, 10, 5);
        Iterator<MailboxItem> mail = store.getMail(users, new NoopUserTimeUpdater()).iterator();
        for (String folder : new String[] { "BOB-FOLDER", "ALICE-FOLDER" })
        {
            for (int i = 0; i < 2; i++)
            {
                assertTrue(mail.hasNext());
                MailboxItem item = mail.next();
                assertEquals(folder + ":the #" + i + " id", item.getHeader("Item ID"));
            }
        }
        assertFalse(mail.hasNext());
    }

    @Test
    public void testGetMailWithStartDates() throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        FolderType aliceFolder = mock(FolderType.class);
        FolderIdType aliceId = mock(FolderIdType.class);
        when(aliceFolder.isSetFolderId()).thenReturn(true);
        when(aliceFolder.getFolderId()).thenReturn(aliceId);
        when(aliceId.getId()).thenReturn("ALICE-FOLDER");

        mockFindFolders(new FolderType[] { aliceFolder }, "alice");
        MessageType[] allMessages = mockFindItem("ALICE-FOLDER", 0, 10, 5, "alice");
        mockGetItem(allMessages, generateIds(0, 5, "ALICE-FOLDER"), "alice");

        MessageType[] newMessages = new MessageType[2];
        newMessages[0] = allMessages[3];
        newMessages[1] = allMessages[4];
        mockFindItem(newMessages, "ALICE-FOLDER", 0, 10, "alice", new DateTime(3 * 1000));
        mockGetItem(newMessages, generateIds(3, 2, "ALICE-FOLDER"), "alice");

        UserTimeUpdater mockTimeUpdater = mock(UserTimeUpdater.class);
        when(mockTimeUpdater.lastUpdated("alice")).thenReturn(new DateTime(0));

        ArrayList<String> users = new ArrayList<String>();
        users.add("alice");

        ExchangeMailStore store = new ExchangeMailStore(service, 10, 5);
        Iterator<MailboxItem> mail = store.getMail(users, mockTimeUpdater).iterator();
        for (int i = 0; i < 5; i++)
        {
            assertTrue(mail.hasNext());
            MailboxItem item = mail.next();
            assertEquals("ALICE-FOLDER:the #" + i + " id", item.getHeader("Item ID"));
        }
        assertFalse(mail.hasNext());

        when(mockTimeUpdater.lastUpdated("alice")).thenReturn(new DateTime(3 * 1000));

        mail = store.getMail(users, mockTimeUpdater).iterator();
        for (int i = 3; i < 5; i++)
        {
            assertTrue(mail.hasNext());
            MailboxItem item = mail.next();
            assertEquals("ALICE-FOLDER:the #" + i + " id", item.getHeader("Item ID"));
        }
        assertFalse(mail.hasNext());
    }
}
