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
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.SyncFolderItemsChangesType;
import com.microsoft.schemas.exchange.services.x2006.types.SyncFolderItemsCreateOrUpdateType;
import com.ripariandata.timberwolf.InMemoryUserFolderSyncStateStorage;

import java.util.List;
import java.util.Vector;

import org.junit.Test;

import static com.ripariandata.timberwolf.exchange.IsXmlBeansRequest.likeThis;
import static com.ripariandata.timberwolf.exchange.SyncFolderItemsHelper.SyncFolderItemsResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Test class for all the SyncFolderItems specific stuff. */
public class SyncFolderItemsTest extends ExchangeTestBase
{

    private static final int DEFAULT_MAX_ENTRIES = 512;
    private final String folderId = "A super unique folder id";
    private final FolderContext folderContext =
            new FolderContext(folderId, getDefaultUser(), new InMemoryUserFolderSyncStateStorage());

    @Test
    public void testGetSyncFolderItemsRequest() throws ServiceCallException
    {
        SyncFolderItemsType syncFolderItems = SyncFolderItemsType.Factory.newInstance();
        syncFolderItems.setMaxChangesReturned(498);
        syncFolderItems.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        syncFolderItems.addNewSyncFolderId().addNewFolderId().setId(folderId);
        syncFolderItems.setSyncState("");

        Configuration config = new Configuration(498, 0);
        FolderContext folder = folderContext(getDefaultUser(), folderId);
        assertEquals(syncFolderItems.xmlText(),
                     SyncFolderItemsHelper.getSyncFolderItemsRequest(config, folder).xmlText());
    }

    @Test
    public void testGetSyncFolderItemsRequestWithState() throws ServiceCallException
    {
        SyncFolderItemsType syncFolderItems = SyncFolderItemsType.Factory.newInstance();
        syncFolderItems.setMaxChangesReturned(512);
        syncFolderItems.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        syncFolderItems.addNewSyncFolderId().addNewFolderId().setId("MySweetExchangeId");
        syncFolderItems.setSyncState("MySweetSyncToken");

        Configuration config = new Configuration(4000, 200);
        FolderContext folder = new FolderContext("MySweetExchangeId", getDefaultUser(),
                                                 new InMemoryUserFolderSyncStateStorage());
        folder.setSyncStateToken("MySweetSyncToken");
        assertEquals(syncFolderItems.xmlText(),
                     SyncFolderItemsHelper.getSyncFolderItemsRequest(config, folder).xmlText());
    }

    private void assertSyncFolderItemsRequestMaxEntries(final int maxItems) throws ServiceCallException
    {
        Configuration config = new Configuration(maxItems, 0);
        SyncFolderItemsType request = SyncFolderItemsHelper.getSyncFolderItemsRequest(config, folderContext);
        assertEquals(Math.max(1, maxItems), request.getMaxChangesReturned());
    }

    @Test
    public void testGetSyncFolderItemsRequestMaxEntries() throws ServiceCallException
    {
        final int maxEntries1 = 10;
        assertSyncFolderItemsRequestMaxEntries(maxEntries1);
        final int maxEntries2 = 3;
        assertSyncFolderItemsRequestMaxEntries(maxEntries2);
        final int maxEntries3 = 0;
        assertSyncFolderItemsRequestMaxEntries(maxEntries3);
        final int maxEntries4 = 1;
        assertSyncFolderItemsRequestMaxEntries(maxEntries4);
    }

    @Test
    public void testSyncFolderItemsInboxRespondNull() throws ServiceCallException, HttpErrorException
    {
        Configuration config = new Configuration(DEFAULT_MAX_ENTRIES, 0);
        SyncFolderItemsType syncItems = SyncFolderItemsHelper.getSyncFolderItemsRequest(config, folderContext);
        when(getService().syncFolderItems(likeThis(syncItems), eq(getDefaultUser()))).thenReturn(null);
        try
        {
            SyncFolderItemsResult result =
                    SyncFolderItemsHelper.syncFolderItems(getService(), config, folderContext);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("Null response from Exchange service.", e.getMessage());
        }
    }

    @Test
    public void testSyncFolderItemsRespond0() throws ServiceCallException, HttpErrorException
    {
        String[] ids = new String[0];
        final String myNewSyncState = "MyNewSyncState";
        mockSyncFolderItems(ids, myNewSyncState);
        SyncFolderItemsResult result = SyncFolderItemsHelper
                .syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        assertEquals(0, result.getIds().size());
        assertTrue(result.includesLastItem());
        assertEquals("", getDefaultFolder().getSyncStateToken());
        assertEquals(myNewSyncState, result.getSyncState());
    }

    @Test
    public void testSyncFolderItems1() throws ServiceCallException, HttpErrorException
    {
        String[] ids = new String[]{"onlyId"};
        final String oldSyncState = "oldSyncState";
        final String newSyncState = "MySweetNewSyncState";
        getDefaultFolder().setSyncStateToken(oldSyncState);
        mockSyncFolderItems(ids, newSyncState);
        SyncFolderItemsResult result =
                SyncFolderItemsHelper.syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        Vector<String> expected = new Vector<String>(1);
        expected.add("onlyId");
        assertEquals(expected, result.getIds());
        assertTrue(result.includesLastItem());
        assertEquals(oldSyncState, getDefaultFolder().getSyncStateToken());
        assertEquals(newSyncState, result.getSyncState());
    }

    @Test
    public void testSyncFolderItems100() throws ServiceCallException, HttpErrorException
    {
        final int count = 100;
        List<String> ids = generateIds(0, count, getDefaultFolderId());
        final String oldSyncState = "oldSyncState";
        final String newSyncState = "MySweetNewSyncState";
        getDefaultFolder().setSyncStateToken(oldSyncState);
        mockSyncFolderItems(ids.toArray(new String[ids.size()]), newSyncState);
        SyncFolderItemsResult result =
                SyncFolderItemsHelper.syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        assertEquals(ids, result.getIds());
        assertTrue(result.includesLastItem());
        assertEquals(oldSyncState, getDefaultFolder().getSyncStateToken());
        assertEquals(newSyncState, result.getSyncState());
    }

    @Test
    public void testSyncFolderItemsRespond0WithMore() throws ServiceCallException, HttpErrorException
    {
        MessageType[] messages = new MessageType[0];
        final String myNewSyncState = "MyNewSyncState";
        mockSyncFolderItems(messages, getDefaultFolder(),
                            getDefaultConfig().getFindItemPageSize(), myNewSyncState, false);
        SyncFolderItemsResult result = SyncFolderItemsHelper
                .syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        assertEquals(0, result.getIds().size());
        assertFalse(result.includesLastItem());
        assertEquals("", getDefaultFolder().getSyncStateToken());
        assertEquals(myNewSyncState, result.getSyncState());
    }

    @Test
    public void testSyncFolderItems100WithMore() throws ServiceCallException, HttpErrorException
    {
        final int count = 100;
        List<String> ids = generateIds(0, count, getDefaultFolderId());
        MessageType[] messages = createMockMessages(getDefaultFolderId(), 0, count);
        final String oldSyncState = "oldSyncState";
        final String newSyncState = "MySweetNewSyncState";
        getDefaultFolder().setSyncStateToken(oldSyncState);
        mockSyncFolderItems(messages, getDefaultFolder(), getDefaultConfig().getFindItemPageSize(),
                            newSyncState, false);
        SyncFolderItemsResult result =
                SyncFolderItemsHelper.syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        assertEquals(ids, result.getIds());
        assertFalse(result.includesLastItem());
        assertEquals(oldSyncState, getDefaultFolder().getSyncStateToken());
        assertEquals(newSyncState, result.getSyncState());
    }

    @Test
    public void testNoMessages() throws ServiceCallException, HttpErrorException
    {
        final String oldState = "old sync state";
        getDefaultFolder().setSyncStateToken("old sync state");

        SyncFolderItemsType syncItems = SyncFolderItemsHelper.getSyncFolderItemsRequest(getDefaultConfig(),
                                                                                        getDefaultFolder());

        SyncFolderItemsResponseType syncItemsResponse = mock(SyncFolderItemsResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);

        when(getService().syncFolderItems(likeThis(syncItems), eq(getDefaultFolder().getUser()))).
                thenReturn(syncItemsResponse);
        when(syncItemsResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getSyncFolderItemsResponseMessageArray())
                .thenReturn(new SyncFolderItemsResponseMessageType[]{});
        SyncFolderItemsResult result = SyncFolderItemsHelper
                .syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        assertEquals(0, result.getIds().size());
        assertTrue(result.includesLastItem());
        assertEquals(oldState, getDefaultFolder().getSyncStateToken());
    }

    @Test
    public void testErrorResponseCode() throws ServiceCallException, HttpErrorException
    {
        final String oldState = "old sync state";
        getDefaultFolder().setSyncStateToken(oldState);
        SyncFolderItemsType syncItems = SyncFolderItemsHelper.getSyncFolderItemsRequest(getDefaultConfig(),
                                                                                        getDefaultFolder());

        SyncFolderItemsResponseType syncItemsResponse = mock(SyncFolderItemsResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        SyncFolderItemsResponseMessageType syncFolderItemsResponseMessage =
                mock(SyncFolderItemsResponseMessageType.class);
        SyncFolderItemsChangesType syncFolderItemsChanges = mock(SyncFolderItemsChangesType.class);

        when(getService().syncFolderItems(likeThis(syncItems), eq(getDefaultFolder().getUser()))).
                thenReturn(syncItemsResponse);
        when(syncItemsResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getSyncFolderItemsResponseMessageArray())
                .thenReturn(new SyncFolderItemsResponseMessageType[]{syncFolderItemsResponseMessage});
        when(syncFolderItemsResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.ERROR_ACCESS_DENIED);
        try
        {
            SyncFolderItemsResult result = SyncFolderItemsHelper
                    .syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
            fail("No exception was thrown");
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response contained an error.", e.getMessage());
        }
        assertEquals(oldState, getDefaultFolder().getSyncStateToken());
    }

    @Test
    public void testUnsetIncludesLastItemInRange() throws ServiceCallException, HttpErrorException
    {
        String[] ids = new String[]{"onlyId"};
        final String oldSyncState = "oldSyncState";
        final String newSyncState = "MySweetNewSyncState";
        getDefaultFolder().setSyncStateToken(oldSyncState);
        SyncFolderItemsType syncItems = SyncFolderItemsHelper.getSyncFolderItemsRequest(getDefaultConfig(),
                                                                                        getDefaultFolder());

        SyncFolderItemsResponseType syncItemsResponse = mock(SyncFolderItemsResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        SyncFolderItemsResponseMessageType syncFolderItemsResponseMessage =
                mock(SyncFolderItemsResponseMessageType.class);
        SyncFolderItemsChangesType syncFolderItemsChanges = mock(SyncFolderItemsChangesType.class);

        when(getService().syncFolderItems(likeThis(syncItems), eq(getDefaultFolder().getUser())))
                .thenReturn(syncItemsResponse);
        when(syncItemsResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getSyncFolderItemsResponseMessageArray())
                .thenReturn(new SyncFolderItemsResponseMessageType[]{syncFolderItemsResponseMessage});
        when(syncFolderItemsResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(syncFolderItemsResponseMessage.isSetIncludesLastItemInRange()).thenReturn(false);
        when(syncFolderItemsResponseMessage.isSetSyncState()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getSyncState()).thenReturn(newSyncState);
        when(syncFolderItemsResponseMessage.isSetChanges()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getChanges()).thenReturn(syncFolderItemsChanges);
        SyncFolderItemsCreateOrUpdateType[] creates = createSyncFolderItemsCreateArray(ids);
        when(syncFolderItemsChanges.getCreateArray()).thenReturn(creates);
        SyncFolderItemsResult result =
                SyncFolderItemsHelper.syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        Vector<String> expected = new Vector<String>(1);
        expected.add("onlyId");
        assertEquals(expected, result.getIds());
        assertFalse(result.includesLastItem());
        assertEquals(oldSyncState, getDefaultFolder().getSyncStateToken());
        assertEquals(newSyncState, result.getSyncState());
    }

    @Test
    public void testUnsetSyncState() throws ServiceCallException, HttpErrorException
    {
        String[] ids = new String[]{"onlyId"};
        final String oldSyncState = "oldSyncState";
        getDefaultFolder().setSyncStateToken(oldSyncState);
        SyncFolderItemsType syncItems = SyncFolderItemsHelper.getSyncFolderItemsRequest(getDefaultConfig(),
                                                                                        getDefaultFolder());

        SyncFolderItemsResponseType syncItemsResponse = mock(SyncFolderItemsResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        SyncFolderItemsResponseMessageType syncFolderItemsResponseMessage =
                mock(SyncFolderItemsResponseMessageType.class);
        SyncFolderItemsChangesType syncFolderItemsChanges = mock(SyncFolderItemsChangesType.class);

        when(getService().syncFolderItems(likeThis(syncItems), eq(getDefaultFolder().getUser())))
                .thenReturn(syncItemsResponse);
        when(syncItemsResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getSyncFolderItemsResponseMessageArray())
                .thenReturn(new SyncFolderItemsResponseMessageType[]{syncFolderItemsResponseMessage});
        when(syncFolderItemsResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(syncFolderItemsResponseMessage.isSetIncludesLastItemInRange()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getIncludesLastItemInRange()).thenReturn(false);
        when(syncFolderItemsResponseMessage.isSetSyncState()).thenReturn(false);
        when(syncFolderItemsResponseMessage.isSetChanges()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getChanges()).thenReturn(syncFolderItemsChanges);
        SyncFolderItemsCreateOrUpdateType[] creates = createSyncFolderItemsCreateArray(ids);
        when(syncFolderItemsChanges.getCreateArray()).thenReturn(creates);
        SyncFolderItemsResult result =
                SyncFolderItemsHelper.syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        Vector<String> expected = new Vector<String>(1);
        expected.add("onlyId");
        assertEquals(expected, result.getIds());
        assertFalse(result.includesLastItem());
        assertEquals(oldSyncState, getDefaultFolder().getSyncStateToken());
        assertEquals(oldSyncState, result.getSyncState());
    }

    @Test
    public void testNoChanges() throws ServiceCallException, HttpErrorException
    {
        String[] ids = new String[0];
        final String myNewSyncState = "MyNewSyncState";
        SyncFolderItemsType syncItems = SyncFolderItemsHelper.getSyncFolderItemsRequest(getDefaultConfig(),
                                                                                        getDefaultFolder());

        SyncFolderItemsResponseType syncItemsResponse = mock(SyncFolderItemsResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        SyncFolderItemsResponseMessageType syncFolderItemsResponseMessage =
                mock(SyncFolderItemsResponseMessageType.class);

        when(getService().syncFolderItems(likeThis(syncItems), eq(getDefaultFolder().getUser())))
                .thenReturn(syncItemsResponse);
        when(syncItemsResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getSyncFolderItemsResponseMessageArray())
                .thenReturn(new SyncFolderItemsResponseMessageType[]{syncFolderItemsResponseMessage});
        when(syncFolderItemsResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(syncFolderItemsResponseMessage.isSetIncludesLastItemInRange()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getIncludesLastItemInRange()).thenReturn(true);
        when(syncFolderItemsResponseMessage.isSetSyncState()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getSyncState()).thenReturn(myNewSyncState);
        when(syncFolderItemsResponseMessage.isSetChanges()).thenReturn(false);


        SyncFolderItemsResult result = SyncFolderItemsHelper
                .syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        assertEquals(0, result.getIds().size());
        assertTrue(result.includesLastItem());
        assertEquals("", getDefaultFolder().getSyncStateToken());
        assertEquals(myNewSyncState, result.getSyncState());
    }

    @Test
    public void testUnsetMessage() throws ServiceCallException, HttpErrorException
    {
        final int count = 3;
        List<String> ids = generateIds(0, count, getDefaultFolderId());
        final String oldSyncState = "oldSyncState";
        final String newSyncState = "MySweetNewSyncState";
        getDefaultFolder().setSyncStateToken(oldSyncState);
        final SyncFolderItemsCreateOrUpdateType[] creates = new SyncFolderItemsCreateOrUpdateType[3];
        creates[0] = mockCreateItem(ids.get(0));
        creates[1] = mock(SyncFolderItemsCreateOrUpdateType.class);
        when(creates[1].isSetMessage()).thenReturn(false);
        creates[2] = mockCreateItem(ids.get(2));
        mockSyncFolderItems(creates, getDefaultFolder(), getDefaultConfig().getFindItemPageSize(), newSyncState, true);
        SyncFolderItemsResult result =
                SyncFolderItemsHelper.syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        ids.remove(1);
        assertEquals(ids, result.getIds());
        assertTrue(result.includesLastItem());
        assertEquals(oldSyncState, getDefaultFolder().getSyncStateToken());
        assertEquals(newSyncState, result.getSyncState());
    }

    @Test
    public void testUnsetItemId() throws ServiceCallException, HttpErrorException
    {
        final int count = 3;
        List<String> ids = generateIds(0, count, getDefaultFolderId());
        final String oldSyncState = "oldSyncState";
        final String newSyncState = "MySweetNewSyncState";
        getDefaultFolder().setSyncStateToken(oldSyncState);
        final SyncFolderItemsCreateOrUpdateType[] creates = new SyncFolderItemsCreateOrUpdateType[3];
        creates[0] = mockCreateItem(ids.get(0));
        creates[1] = mock(SyncFolderItemsCreateOrUpdateType.class);
        when(creates[1].isSetMessage()).thenReturn(true);
        final MessageType message = mock(MessageType.class);
        when(creates[1].getMessage()).thenReturn(message);
        when(message.isSetItemId()).thenReturn(false);
        creates[2] = mockCreateItem(ids.get(2));
        mockSyncFolderItems(creates, getDefaultFolder(), getDefaultConfig().getFindItemPageSize(), newSyncState, true);
        SyncFolderItemsResult result =
                SyncFolderItemsHelper.syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        ids.remove(1);
        assertEquals(ids, result.getIds());
        assertTrue(result.includesLastItem());
        assertEquals(oldSyncState, getDefaultFolder().getSyncStateToken());
        assertEquals(newSyncState, result.getSyncState());
    }

}
