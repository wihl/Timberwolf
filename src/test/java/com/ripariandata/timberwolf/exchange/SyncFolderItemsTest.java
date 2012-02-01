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

import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;

import java.util.Vector;

import static com.ripariandata.timberwolf.exchange.IsXmlBeansRequest.likeThis;
import static com.ripariandata.timberwolf.exchange.SyncFolderItemsHelper.SyncFolderItemsResult;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

/** Test class for all the SyncFolderItems specific stuff. */
public class SyncFolderItemsTest extends ExchangeTestBase
{

    private static final int DEFAULT_MAX_ENTRIES = 512;
    private FolderContext defaultInboxFolder =
            new FolderContext(DistinguishedFolderIdNameType.INBOX, getDefaultUser(), null);

    @Test
    public void testGetSyncFolderItemsRequest() throws ServiceCallException
    {
        SyncFolderItemsType syncFolderItems = SyncFolderItemsType.Factory.newInstance();
        syncFolderItems.setMaxChangesReturned(498);
        syncFolderItems.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        syncFolderItems.addNewSyncFolderId().addNewDistinguishedFolderId().setId(DistinguishedFolderIdNameType.INBOX);
        syncFolderItems.setSyncState("");

        Configuration config = new Configuration(498, 0);
        FolderContext folder = new FolderContext(DistinguishedFolderIdNameType.INBOX, getDefaultUser(), null);
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
        FolderContext folder = new FolderContext("MySweetExchangeId", getDefaultUser(), "MySweetSyncToken");
        assertEquals(syncFolderItems.xmlText(),
                     SyncFolderItemsHelper.getSyncFolderItemsRequest(config, folder).xmlText());
    }

    private void assertSyncFolderItemsRequestMaxEntries(final int maxItems) throws ServiceCallException
    {
        Configuration config = new Configuration(maxItems, 0);
        SyncFolderItemsType request = SyncFolderItemsHelper.getSyncFolderItemsRequest(config, defaultInboxFolder);
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
        SyncFolderItemsType syncItems = SyncFolderItemsHelper.getSyncFolderItemsRequest(config, defaultInboxFolder);
        when(getService().syncFolderItems(likeThis(syncItems), eq(getDefaultUser()))).thenReturn(null);
        try
        {
            SyncFolderItemsResult result =
                    SyncFolderItemsHelper.syncFolderItems(getService(), config, defaultInboxFolder);
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
        assertEquals(myNewSyncState, getDefaultFolder().getSyncStateToken());
    }

    @Test
    public void testSyncFolderItems1() throws ServiceCallException, HttpErrorException
    {
        String[] ids = new String[]{"onlyId"};
        final String newSyncState = "MySweetNewSyncState";
        getDefaultFolder().setSyncStateToken("oldSyncState");
        mockSyncFolderItems(ids, newSyncState);
        SyncFolderItemsResult result =
                SyncFolderItemsHelper.syncFolderItems(getService(), getDefaultConfig(), getDefaultFolder());
        Vector<String> expected = new Vector<String>(1);
        expected.add("onlyId");
        assertEquals(expected, result.getIds());
        assertTrue(result.includesLastItem());
        assertEquals(newSyncState, getDefaultFolder().getSyncStateToken());
    }


}
