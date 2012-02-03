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

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfFoldersType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.FindFolderParentType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.ripariandata.timberwolf.InMemoryUserFolderSyncStateStorage;
import com.ripariandata.timberwolf.MailboxItem;
import com.ripariandata.timberwolf.UserFolderSyncStateStorage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import org.apache.xmlbeans.XmlException;
import org.junit.Before;
import org.junit.Test;

import static com.ripariandata.timberwolf.exchange.IsXmlBeansRequest.likeThis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Test for ExchangeMailStore, uses mock exchange service. */
public class ExchangeMailStoreTest extends ExchangeTestBase
{
    private final String idHeaderKey = "Item ID";
    private ArrayList<String> defaultUser;

    @Before
    public void setUp() throws Exception
    {
        super.setUp();
        defaultUser = new ArrayList<String>();
        defaultUser.add(getDefaultUser());
    }

    @Test
    public void testGetMailFind0()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException
    {
        // Exchange returns 0 mail when findItem is called
        defaultMockFindFolders();
        mockSyncFolderItems(new String[0], "newSyncState");
        ExchangeMailStore store = new ExchangeMailStore(getService());
        for (MailboxItem mailboxItem : store.getMail(defaultUser, new InMemoryUserFolderSyncStateStorage()))
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
        ExchangeMailStore store = new ExchangeMailStore(getService());
        for (MailboxItem mailboxItem : store.getMail(defaultUser, new InMemoryUserFolderSyncStateStorage()))
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
        final int count = 100;
        String[] ids = new String[count];
        for (int i = 0; i < count; i++)
        {
            ids[i] = "the" + i + "id";
        }
        mockSyncFolderItems(ids, "new sync state");
        defaultMockFindFolders();

        try
        {
            ExchangeMailStore store = new ExchangeMailStore(getService());
            Iterable<MailboxItem> mail = store.getMail(defaultUser, new InMemoryUserFolderSyncStateStorage());
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
        // Exchange returns 30 in SyncFolderItems and 30 in GetItems
        final int count = 30;
        List<String> requestedList = generateIds(0, count, getDefaultFolderId());
        defaultMockFindFolders();
        MessageType[] messages = mockSyncFolderItems(0, 512, count, "newSyncState");
        mockGetItem(messages, 0, count, 0, count, getDefaultFolderId());
        int i = 0;
        ExchangeMailStore store = new ExchangeMailStore(getService());
        for (MailboxItem mailboxItem : store.getMail(defaultUser, new InMemoryUserFolderSyncStateStorage()))
        {
            assertEquals(requestedList.get(i), mailboxItem.getHeader(idHeaderKey));
            i++;
        }
        if (i < requestedList.size())
        {
            fail("There were less items returned than there should have been");
        }
    }

    private void assertFolderSyncState(final int index, final int itemsInExchange, final String expected)
    {
        if (index <= itemsInExchange)
        {
            assertEquals("SyncStateToken at Email: " + index + "/" + itemsInExchange,
                         expected, getDefaultFolder().getSyncStateToken());
        }
    }

    @Test
    public void testSyncFolderItemsOneIdPageTwoItemPages()
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        final int itemsInExchange = 10;
        final int idPageSize = 11;
        final int itemPageSize = 5;
        final String newSyncState = "New Sync State";
        Configuration config = new Configuration(idPageSize, itemPageSize);
        MessageType[] messages = mockSyncFolderItems(0, idPageSize, itemsInExchange, newSyncState);
        mockGetItem(messages, 0, itemPageSize, 0, itemsInExchange, getDefaultFolderId());
        mockGetItem(messages, 0, itemPageSize, 1, itemsInExchange, getDefaultFolderId());

        SyncFolderItemIterator mailIterator = new SyncFolderItemIterator(getService(), config, getDefaultFolder());

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, getDefaultFolderId());
        while (mailIterator.hasNext())
        {
            MailboxItem item = mailIterator.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            index++;
            assertFolderSyncState(index, itemsInExchange, "");
        }
        assertFolderSyncState(itemsInExchange, itemsInExchange, newSyncState);
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testSyncFolderItemsOneIdPageFiveItemPages()
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        final int itemsInExchange = 24;
        final int idPageSize = 30;
        final int itemPageSize = 5;
        final String newSyncState = "New sync state";
        Configuration config = new Configuration(idPageSize, itemPageSize);
        MessageType[] syncResults = mockSyncFolderItems(0, idPageSize, itemsInExchange, newSyncState);
        final int pageIndexCount = 5;
        for (int i = 0; i < pageIndexCount; i++)
        {
            mockGetItem(syncResults, 0, itemPageSize, i, itemsInExchange, getDefaultFolderId());
        }

        SyncFolderItemIterator mailIterator = new SyncFolderItemIterator(getService(), config, getDefaultFolder());

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, getDefaultFolderId());
        while (mailIterator.hasNext())
        {
            MailboxItem item = mailIterator.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            index++;
            assertFolderSyncState(index, itemsInExchange, "");
        }
        assertFolderSyncState(itemsInExchange, itemsInExchange, newSyncState);
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testSyncFolderItemsTwoIdPages10ItemPages()
            throws IOException, ServiceCallException, HttpErrorException, XmlException
    {
        final int itemsInExchange = 50;
        final int idPageSize = 30;
        final int itemPageSize = 5;
        Configuration config = new Configuration(idPageSize, itemPageSize);
        // SyncFolderItem #1
        final String secondSyncState = "The second sync state";
        final String lastSyncState = "The last sync state";
        MessageType[] syncResults = mockSyncFolderItems(0, idPageSize, idPageSize, secondSyncState, false);
        final int syncItemCount = 6;
        for (int i = 0; i < syncItemCount; i++)
        {
            mockGetItem(syncResults, 0, itemPageSize, i, itemsInExchange, getDefaultFolderId());
        }

        // SyncItem #2
        getDefaultFolder().setSyncStateToken(secondSyncState);
        syncResults = mockSyncFolderItems(idPageSize, idPageSize, itemsInExchange - idPageSize, lastSyncState, true);
        final int mockSyncItemCount2 = 4;
        for (int i = 0; i < mockSyncItemCount2; i++)
        {
            mockGetItem(syncResults, idPageSize, itemPageSize, i, itemsInExchange, getDefaultFolderId());
        }

        // set the default folder back to a sync state of null
        getDefaultFolder().setSyncStateToken(null);
        SyncFolderItemIterator mailIterator = new SyncFolderItemIterator(getService(), config, getDefaultFolder());

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, getDefaultFolderId());
        while (mailIterator.hasNext())
        {
            MailboxItem item = mailIterator.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            index++;
            assertFolderSyncState(index, idPageSize, "");
            if (index > idPageSize)
            {
                assertFolderSyncState(index, itemsInExchange, secondSyncState);
            }
        }
        assertFolderSyncState(itemsInExchange, itemsInExchange, lastSyncState);
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testSyncFolderItemsFiveIdPages20ItemPages()
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        final int itemsInExchange = 100;
        final int idPageSize = 20;
        final int itemPageSize = 5;
        Configuration config = new Configuration(idPageSize, itemPageSize);
        final int syncOffsetCount = 5;
        final int getOffsetCount = 4;
        Vector<String> syncStates = new Vector<String>();
        syncStates.add("Original Sync State");
        getDefaultFolder().setSyncStateToken(syncStates.get(0));
        for (int i = 0; i < syncOffsetCount; i++)
        {
            String newSyncState = "SyncState#" + i;
            boolean includesLastItem = i == syncOffsetCount - 1;
            MessageType[] syncResults = mockSyncFolderItems(i * idPageSize, idPageSize, idPageSize, newSyncState,
                                                            includesLastItem);
            syncStates.add(newSyncState);
            getDefaultFolder().setSyncStateToken(newSyncState);
            for (int j = 0; j < getOffsetCount; j++)
            {
                mockGetItem(syncResults, idPageSize * i, itemPageSize, j, itemsInExchange, getDefaultFolderId());
            }
        }

        // reset sync state
        getDefaultFolder().setSyncStateToken(syncStates.get(0));
        SyncFolderItemIterator mailIterator = new SyncFolderItemIterator(getService(), config, getDefaultFolder());

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, getDefaultFolderId());
        String expected = syncStates.remove(0);
        while (mailIterator.hasNext())
        {
            MailboxItem item = mailIterator.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            if (index > 0 && index % idPageSize == 0)
            {
                expected = syncStates.remove(0);
            }
            index++;
            assertFolderSyncState(index, itemsInExchange, expected);
        }
        assertEquals(syncStates.remove(0), getDefaultFolder().getSyncStateToken());
        assertEquals("Whoops, i think the test is messed up, syncStates should be empty", 0, syncStates.size());
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testSyncFolderItemsItemPageLargerThanIdPage()
            throws IOException, ServiceCallException,
                   HttpErrorException, XmlException
    {
        final int itemsInExchange = 20;
        final int idPageSize = 5;
        final int itemPageSize = 10;
        Configuration config = new Configuration(idPageSize, itemPageSize);
        final int offsetCount = 4;
        Vector<String> syncStates = new Vector<String>();
        syncStates.add("");
        for (int i = 0; i < offsetCount; i++)
        {
            String newSyncState = "SyncState#" + i;
            boolean includesLastItem = i == offsetCount - 1;
            MessageType[] syncResults = mockSyncFolderItems(i * idPageSize, idPageSize, idPageSize, newSyncState,
                                                            includesLastItem);
            syncStates.add(newSyncState);
            getDefaultFolder().setSyncStateToken(newSyncState);
            mockGetItem(syncResults, idPageSize * i, idPageSize, 0, itemsInExchange, getDefaultFolderId());
        }

        // reset sync state
        getDefaultFolder().setSyncStateToken(null);
        SyncFolderItemIterator mailIterator = new SyncFolderItemIterator(getService(), config, getDefaultFolder());

        int index = 0;
        List<String> ids = generateIds(0, itemsInExchange, getDefaultFolderId());
        String expected = syncStates.remove(0);
        while (mailIterator.hasNext())
        {
            MailboxItem item = mailIterator.next();
            assertEquals(ids.get(index), item.getHeader("Item ID"));
            if (index > 0 && index % idPageSize == 0)
            {
                expected = syncStates.remove(0);
            }
            index++;
            assertFolderSyncState(index, itemsInExchange, expected);
        }
        assertEquals(syncStates.remove(0), getDefaultFolder().getSyncStateToken());
        assertEquals("Whoops, i think the test is messed up, syncStates should be empty", 0, syncStates.size());
        assertEquals(itemsInExchange, index);
    }

    @Test
    public void testGetMailWithPagingAndFolders() throws ServiceCallException, HttpErrorException, XmlException,
                                                         IOException
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

        when(folders.getFolderArray()).thenReturn(new FolderType[]{folderOne, folderTwo, folderThree});
        when(parent.getFolders()).thenReturn(folders);
        when(folderMsgs.getRootFolder()).thenReturn(parent);
        when(folderMsgs.isSetRootFolder()).thenReturn(true);
        FindFolderResponseMessageType[] fFRMT = new FindFolderResponseMessageType[]{folderMsgs};
        when(folderArr.getFindFolderResponseMessageArray()).thenReturn(fFRMT);
        when(folderResponse.getResponseMessages()).thenReturn(folderArr);

        when(getService().findFolder(likeThis(FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType
                                                                                             .MSGFOLDERROOT)),
                                     eq(getDefaultUser()))).thenReturn(folderResponse);
        final int offsetZero = 0;
        final int offsetFive = 5;
        final int offsetTen = 10;
        final int maxIdTen = 10;
        final int countTwo = 2;
        final int countThree = 3;
        final int countFive = 5;
        final int countTen = 10;
        mockSyncFolderItems("FOLDER-ONE-ID", offsetZero, maxIdTen, countTwo, "", "FOLDER-ONE-SYNC2", true);
        mockGetItem(new MessageType[]{mockMessageItemId("FOLDER-ONE-ID:the #0 id"),
                mockMessageItemId("FOLDER-ONE-ID:the #1 id")},
                    generateIds(offsetZero, countTwo, "FOLDER-ONE-ID"));
        mockSyncFolderItems("FOLDER-TWO-ID", offsetZero, maxIdTen, countTen, null, "FOLDER-TWO-SYNC2", false);
        mockGetItem(new MessageType[]{mockMessageItemId("FOLDER-TWO-ID:the #0 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #1 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #2 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #3 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #4 id")},
                    generateIds(offsetZero, countFive, "FOLDER-TWO-ID"));
        mockGetItem(new MessageType[]{mockMessageItemId("FOLDER-TWO-ID:the #5 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #6 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #7 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #8 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #9 id")},
                    generateIds(offsetFive, countFive, "FOLDER-TWO-ID"));
        mockSyncFolderItems("FOLDER-TWO-ID", offsetTen, maxIdTen, countThree, "FOLDER-TWO-SYNC2", "FOLDER-TWO-SYNC3",
                            true);
        mockGetItem(new MessageType[]{mockMessageItemId("FOLDER-TWO-ID:the #10 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #11 id"),
                mockMessageItemId("FOLDER-TWO-ID:the #12 id")},
                    generateIds(offsetTen, countThree, "FOLDER-TWO-ID"));
        mockSyncFolderItems("FOLDER-THREE-ID", offsetZero, maxIdTen, countTwo, "", "FOLDER-THREE-SYNC2",
                            true);
        mockGetItem(new MessageType[]{mockMessageItemId("FOLDER-THREE-ID:the #0 id"),
                mockMessageItemId("FOLDER-THREE-ID:the #1 id")},
                    generateIds(offsetZero, countTwo, "FOLDER-THREE-ID"));

        final int findItemPageSize = 10;
        final int getItemPageSize = 5;
        ExchangeMailStore store = new ExchangeMailStore(getService(), findItemPageSize, getItemPageSize);
        Iterator<MailboxItem> mail = store.getMail(defaultUser, new InMemoryUserFolderSyncStateStorage()).iterator();
        final int folderIdTwoCount = 13;
        final int folderIdOtherCount = 2;
        for (String folder : new String[]{"FOLDER-ONE-ID", "FOLDER-TWO-ID", "FOLDER-THREE-ID"})
        {
            for (int i = 0; i < (folder == "FOLDER-TWO-ID" ? folderIdTwoCount : folderIdOtherCount); i++)
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
        final String aliceFolderId = "ALICE-FOLDER";
        final String bobFolderId = "BOB-FOLDER";
        FolderType aliceFolder = mock(FolderType.class);
        FolderIdType aliceId = mock(FolderIdType.class);
        when(aliceFolder.isSetFolderId()).thenReturn(true);
        when(aliceFolder.getFolderId()).thenReturn(aliceId);
        when(aliceId.getId()).thenReturn(aliceFolderId);

        FolderType bobFolder = mock(FolderType.class);
        FolderIdType bobId = mock(FolderIdType.class);
        when(bobFolder.isSetFolderId()).thenReturn(true);
        when(bobFolder.getFolderId()).thenReturn(bobId);
        when(bobId.getId()).thenReturn(bobFolderId);

        mockFindFolders(new FolderType[]{aliceFolder}, "alice");
        mockFindFolders(new FolderType[]{bobFolder}, "bob");
        final int maxIdCount = 10;
        final int folderCount = 2;
        mockSyncFolderItems(createMockMessages(aliceFolderId, 0, folderCount),
                            folderContext("alice", aliceFolderId),
                            maxIdCount, "ALICE-SYNC2", true);
        mockGetItem(new MessageType[]
                    {
                            mockMessageItemId(aliceFolderId + ":the #0 id"),
                            mockMessageItemId("ALICE-FOLDER:the #1 id")
                    },
                    generateIds(0, folderCount, aliceFolderId), "alice");
        mockSyncFolderItems(createMockMessages(bobFolderId, 0, folderCount),
                            folderContext("bob", bobFolderId),
                            maxIdCount, "BOB-SYNC2", true);
        mockGetItem(new MessageType[]
                    {
                            mockMessageItemId("BOB-FOLDER:the #0 id"),
                            mockMessageItemId("BOB-FOLDER:the #1 id")
                    },
                    generateIds(0, folderCount, bobFolderId), "bob");

        ArrayList<String> users = new ArrayList<String>();
        users.add("bob");
        users.add("alice");

        final int findItemPageSize = 10;
        final int getItemPageSize = 5;
        ExchangeMailStore store = new ExchangeMailStore(getService(), findItemPageSize, getItemPageSize);
        Iterator<MailboxItem> mail = store.getMail(users, new InMemoryUserFolderSyncStateStorage()).iterator();

        final int count = 2;
        for (String folder : new String[]{bobFolderId, aliceFolderId})
        {
            for (int i = 0; i < count; i++)
            {
                assertTrue(mail.hasNext());
                MailboxItem item = mail.next();
                assertEquals(folder + ":the #" + i + " id", item.getHeader("Item ID"));
            }
        }
        assertFalse(mail.hasNext());
    }

    @Test
    public void testGetMailWithSyncStates() throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        final String aliceFolderId = "ALICE-FOLDER";
        final String aliceUsername = "alice";
        final int findItemPageSize = 10;
        final int getItemPageSize = 5;
        final int totalMessageCount = 5;

        FolderType aliceFolder = mock(FolderType.class);
        FolderIdType aliceId = mock(FolderIdType.class);
        when(aliceFolder.isSetFolderId()).thenReturn(true);
        when(aliceFolder.getFolderId()).thenReturn(aliceId);
        when(aliceId.getId()).thenReturn(aliceFolderId);

        mockFindFolders(new FolderType[]{aliceFolder}, aliceUsername);

        UserFolderSyncStateStorage mockSyncStates = mock(UserFolderSyncStateStorage.class);


        // First call to get mail
        when(mockSyncStates.getLastSyncState(aliceUsername, aliceFolderId)).thenReturn(null);

        MessageType[] firstMessages = mockSyncFolderItems(createMockMessages(aliceFolderId, 0, totalMessageCount),
                                                          folderContext(aliceUsername, aliceFolderId),
                                                          findItemPageSize, "syncState2", true);
        mockGetItem(firstMessages, generateIds(0, totalMessageCount, aliceFolderId), aliceUsername);


        ArrayList<String> users = new ArrayList<String>();
        users.add(aliceUsername);

        ExchangeMailStore store = new ExchangeMailStore(getService(), findItemPageSize, getItemPageSize);
        Iterator<MailboxItem> mail = store.getMail(users, mockSyncStates).iterator();
        for (int i = 0; i < totalMessageCount; i++)
        {
            assertTrue(mail.hasNext());
            MailboxItem item = mail.next();
            assertEquals("ALICE-FOLDER:the #" + i + " id", item.getHeader("Item ID"));
        }
        assertFalse(mail.hasNext());
        verify(mockSyncStates).setSyncState(aliceUsername, aliceFolderId, "syncState2");

        when(mockSyncStates.getLastSyncState(aliceUsername, aliceFolderId)).thenReturn("newSyncState");
        MessageType[] newMessages =
                mockSyncFolderItems(createMockMessages(aliceFolderId, 10, 2),
                                    folderContext(aliceUsername, aliceFolderId, "newSyncState"),
                                    findItemPageSize, "lastSyncState", true);
        mockGetItem(newMessages,
                    generateIds(10, 2, aliceFolderId),
                    aliceUsername);


        mail = store.getMail(users, mockSyncStates).iterator();
        for (int i = 10; i < 12; i++)
        {
            assertTrue(mail.hasNext());
            MailboxItem item = mail.next();
            assertEquals("ALICE-FOLDER:the #" + i + " id", item.getHeader("Item ID"));
        }
        assertFalse(mail.hasNext());
        verify(mockSyncStates).setSyncState(aliceUsername, aliceFolderId, "lastSyncState");
    }
}
