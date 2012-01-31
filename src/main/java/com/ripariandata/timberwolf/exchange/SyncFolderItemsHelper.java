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
import java.util.Vector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains helper methods for SyncFolderItems requests.
 */
public final class SyncFolderItemsHelper
{
    private static final Logger LOG = LoggerFactory.getLogger(SyncFolderItemsHelper.class);

    /**
     * Enforces not being able to create an instance.
     */
    private SyncFolderItemsHelper()
    {

    }

    /**
     * Creates a SyncFolderItemsType to request all the new items under the
     * given folder context.
     * @param config The configuration for this instance of Timberwolf.
     * @param folder The current folder being searched.
     * @param syncState The sync state from the last time SyncFolderItems was
     * requested. This can be null.
     * @return The SyncFolderItemsType necessary to request new items from
     * the given folder since the given sync state.
     */
    public SyncFolderItemsType getSyncFolderItemsRequest(final Configuration config, final FolderContext folder,
                                                         final String syncState)
    {
        return null;
    }

    /**
     * Gets a list of all the new ids for the given folder of the current user.
     * @param exchangeService The actual service to use when requesting ids.
     * @param config The configuration for this instance of Timberwolf.
     * @param folder The folder to sync.
     * @param syncState The state, the last time the given folder was synced. This may be null.
     * @return The SyncFolderItems result return from Exchange.
     */
    public Vector<String> syncFolderItems(final ExchangeService exchangeService,
                                          final Configuration config,
                                          final FolderContext folder,
                                          final String syncState)
    {
        return syncFolderItems(exchangeService,  getSyncFolderItemsRequest(config, folder, syncState), folder.getUser());
    }

    /**
     * Gets a list of all the new ids for the given folder of the current user.
     * @param exchangeService The actual service to use when requesting ids.
     * @param syncFolderItemsRequest The request to send to exchange.
     * @param targetUser The user to impersonate for this request.
     * @return The SyncFolderItems result return from Exchange.
     */
    private Vector<String> syncFolderItems(final ExchangeService exchangeService,
                                           final SyncFolderItemsType syncFolderItemsRequest, final String targetUser)
    {
        return null;
    }

}
