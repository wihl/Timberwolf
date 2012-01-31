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
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/** Test class for all the FindItem specific stuff. */
public class SyncFolderItemsTest extends ExchangeTestBase
{

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

}
