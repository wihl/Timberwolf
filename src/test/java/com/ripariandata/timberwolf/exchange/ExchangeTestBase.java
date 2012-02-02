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
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.SyncFolderItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfFoldersType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfRealItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.FindFolderParentType;
import com.microsoft.schemas.exchange.services.x2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.SyncFolderItemsChangesType;
import com.microsoft.schemas.exchange.services.x2006.types.SyncFolderItemsCreateOrUpdateType;
import com.ripariandata.timberwolf.UserTimeUpdater;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.xmlbeans.XmlException;
import org.joda.time.DateTime;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.ripariandata.timberwolf.exchange.IsXmlBeansRequest.likeThis;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


/** Base class for fixtures that need to mock out Exchange services. */
public class ExchangeTestBase
{

    private static final Logger LOG = LoggerFactory.getLogger(ExchangeTestBase.class);
    @Mock
    private ExchangeService service;

    protected ExchangeService getService()
    {
        return service;
    }

    /** This is the name of our default folder. */
    private static final String DEFAULT_FOLDER_ID = "ANAMAZINGLYENGLISH-LIKEGUID";

    protected String getDefaultFolderId()
    {
        return DEFAULT_FOLDER_ID;
    }

    private final String defaultUser = "bkerr";

    protected String getDefaultUser()
    {
        return defaultUser;
    }

    /**
     * This is needed anytime we'd like to look in a particular folder with mockFindItem.
     * This is reset at the start of the test, but otherwise syncState is maintained
     * throughout the test.
     */
    private FolderContext defaultFolder;

    protected FolderContext getDefaultFolder()
    {
        return defaultFolder;
    }

    private static final int DEFAULT_PAGE_SIZE = 1000;

    /** This configuration is used anytime we just need any standard configuration. */
    private Configuration defaultConfig = new Configuration(DEFAULT_PAGE_SIZE, DEFAULT_PAGE_SIZE);

    protected Configuration getDefaultConfig()
    {
        return defaultConfig;
    }

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        defaultFolder = new FolderContext(DEFAULT_FOLDER_ID, defaultUser);
    }

    protected void mockFindItem(final MessageType[] messages)
            throws ServiceCallException, HttpErrorException
    {
        mockFindItem(messages, DEFAULT_FOLDER_ID, 0, DEFAULT_PAGE_SIZE, defaultUser);
    }

    protected List<String> generateIds(final int offset, final int count, final String folder)
    {
        List<String> ids = new ArrayList<String>(count);
        for (int i = offset; i < offset + count; i++)
        {
            ids.add(folder + ":the #" + i + " id");
        }
        return ids;
    }

    protected MessageType[] mockFindItem(final String folder, final int offset, final int maxIds, final int count)
            throws ServiceCallException, HttpErrorException
    {
        return mockFindItem(folder, offset, maxIds, count, defaultUser);
    }

    protected MessageType[] mockFindItem(final String folder, final int offset, final int maxIds, final int count,
                                         final String user) throws ServiceCallException, HttpErrorException
    {
        MessageType[] findItems = new MessageType[count];
        List<String> ids = generateIds(offset, count, folder);
        for (int i = 0; i < count; i++)
        {
            findItems[i] = mockMessageItemId(ids.get(i));
        }
        mockFindItem(findItems, folder, offset, maxIds, user);
        return findItems;
    }

    private void mockFindItem(final MessageType[] messages, final String folder, final int offset,
                              final int maxIds, final String user) throws ServiceCallException, HttpErrorException
    {
        mockFindItem(messages, folder, offset, maxIds, user, new DateTime(0));
    }

    protected void mockFindItem(final MessageType[] messages, final String folder, final int offset, final int maxIds,
                                final String user, final DateTime startDate)
            throws ServiceCallException, HttpErrorException
    {
        Configuration config = new Configuration(maxIds, 0);
        UserTimeUpdater timeUpdater = mock(UserTimeUpdater.class);
        when(timeUpdater.lastUpdated(user)).thenReturn(startDate);
        config = config.withTimeUpdater(timeUpdater);

        FolderContext folderContext = new FolderContext(folder, user);
        FindItemType findItem = FindItemHelper.getFindItemsRequest(config, folderContext, offset);
        FindItemResponseType findItemResponse = mock(FindItemResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        FindItemResponseMessageType findItemResponseMessage = mock(FindItemResponseMessageType.class);
        FindItemParentType findItemParent = mock(FindItemParentType.class);
        ArrayOfRealItemsType arrayOfRealItems = mock(ArrayOfRealItemsType.class);
        when(service.findItem(likeThis(findItem), eq(user))).thenReturn(findItemResponse);
        when(findItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{findItemResponseMessage});
        when(findItemResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findItemResponseMessage.isSetRootFolder()).thenReturn(true);
        when(findItemResponseMessage.getRootFolder()).thenReturn(findItemParent);
        when(findItemParent.isSetItems()).thenReturn(true);
        when(findItemParent.getItems()).thenReturn(arrayOfRealItems);
        when(arrayOfRealItems.getMessageArray()).thenReturn(messages);
    }

    protected void mockSyncFolderItems(final String[] ids, final String newSyncState)
            throws ServiceCallException, HttpErrorException
    {
        mockSyncFolderItems(ids, getDefaultFolder(), getDefaultConfig().getFindItemPageSize(), newSyncState);
    }

    protected MessageType[] mockSyncFolderItems(final int offset, final int maxIds, final int itemsInExchange,
                                                final String newSyncState)
            throws ServiceCallException, HttpErrorException
    {
        return mockSyncFolderItems(offset, maxIds, itemsInExchange, newSyncState, true);
    }


    protected MessageType[] mockSyncFolderItems(final String folderId, final int offset, final int maxIds,
                                                final int itemsInExchange, final String oldSyncState,
                                                final String newSyncState, final boolean includesLastItem)
            throws ServiceCallException, HttpErrorException
    {
        return mockSyncFolderItems(defaultUser, folderId, offset, maxIds, itemsInExchange, oldSyncState, newSyncState,
                                   includesLastItem);
    }

    protected MessageType[] mockSyncFolderItems(final String user, final String folderId, final int offset,
                                                final int maxIds, final int itemsInExchange, final String oldSyncState,
                                                final String newSyncState, final boolean includesLastItem)
            throws ServiceCallException, HttpErrorException
    {
        MessageType[] messages = new MessageType[itemsInExchange];
        List<String> ids = generateIds(offset, itemsInExchange, folderId);
        for (int i = 0; i < itemsInExchange; i++)
        {
            messages[i] = mockMessageItemId(ids.get(i));
        }
        FolderContext folder = new FolderContext(folderId, user);
        folder.setSyncStateToken(oldSyncState);
        mockSyncFolderItems(
                generateIds(offset, itemsInExchange, folderId).toArray(new String[itemsInExchange]),
                folder, maxIds, newSyncState, includesLastItem);
        return messages;
    }

    protected MessageType[] mockSyncFolderItems(final int offset, final int maxIds, final int itemsInExchange,
                                                final String newSyncState, final boolean includesLastItem)
            throws ServiceCallException, HttpErrorException
    {
        MessageType[] messages = new MessageType[itemsInExchange];
        List<String> ids = generateIds(offset, itemsInExchange, getDefaultFolderId());
        for (int i = 0; i < itemsInExchange; i++)
        {
            messages[i] = mockMessageItemId(ids.get(i));
        }
        mockSyncFolderItems(
                generateIds(offset, itemsInExchange, getDefaultFolderId()).toArray(new String[itemsInExchange]),
                getDefaultFolder(), maxIds, newSyncState, includesLastItem);
        return messages;
    }

    protected void mockSyncFolderItems(final String[] ids, final FolderContext folder, final int maxIds,
                                       final String newSyncState)
            throws ServiceCallException, HttpErrorException
    {
        mockSyncFolderItems(ids, folder, maxIds, newSyncState, true);
    }

    protected void mockSyncFolderItems(final String[] ids,
                                       final FolderContext folder,
                                       final int maxIds,
                                       final String newSyncState,
                                       final boolean includesLastItemInRange)
            throws ServiceCallException, HttpErrorException
    {
        mockSyncFolderItems(createSyncFolderItemsCreateArray(ids),
                            folder, maxIds, newSyncState, includesLastItemInRange);
    }

    protected void mockSyncFolderItems(
            final SyncFolderItemsCreateOrUpdateType[] syncFolderItemsCreateOrUpdate,
            final FolderContext folder,
            final int maxIds,
            final String newSyncState,
            final boolean includesLastItemInRange)
            throws ServiceCallException, HttpErrorException
    {
        Configuration config = new Configuration(maxIds, 0);
        SyncFolderItemsType syncItems = SyncFolderItemsHelper.getSyncFolderItemsRequest(config, folder);

        SyncFolderItemsResponseType syncItemsResponse = mock(SyncFolderItemsResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        SyncFolderItemsResponseMessageType syncFolderItemsResponseMessage =
                mock(SyncFolderItemsResponseMessageType.class);
        SyncFolderItemsChangesType syncFolderItemsChanges = mock(SyncFolderItemsChangesType.class);

        LOG.debug("Expecting SyncFolderItems with User:{} Request:\n{}",folder.getUser(), syncItems);
        when(service.syncFolderItems(likeThis(syncItems), eq(folder.getUser()))).thenReturn(syncItemsResponse);
        when(syncItemsResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getSyncFolderItemsResponseMessageArray())
                .thenReturn(new SyncFolderItemsResponseMessageType[]{syncFolderItemsResponseMessage});
        when(syncFolderItemsResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(syncFolderItemsResponseMessage.isSetIncludesLastItemInRange()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getIncludesLastItemInRange()).thenReturn(includesLastItemInRange);
        when(syncFolderItemsResponseMessage.isSetSyncState()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getSyncState()).thenReturn(newSyncState);
        when(syncFolderItemsResponseMessage.isSetChanges()).thenReturn(true);
        when(syncFolderItemsResponseMessage.getChanges()).thenReturn(syncFolderItemsChanges);
        when(syncFolderItemsChanges.getCreateArray()).thenReturn(syncFolderItemsCreateOrUpdate);
    }

    protected SyncFolderItemsCreateOrUpdateType[] createSyncFolderItemsCreateArray(final String[] ids)
    {
        SyncFolderItemsCreateOrUpdateType[] creates = new SyncFolderItemsCreateOrUpdateType[ids.length];
        for (int i = 0; i < ids.length; i++)
        {
            SyncFolderItemsCreateOrUpdateType create = mockCreateItem(ids[i]);
            creates[i] = create;
        }
        return creates;
    }

    protected SyncFolderItemsCreateOrUpdateType mockCreateItem(final String id)
    {
        SyncFolderItemsCreateOrUpdateType create = mock(SyncFolderItemsCreateOrUpdateType.class);
        ItemType item = mock(ItemType.class);
        ItemIdType itemId = mock(ItemIdType.class);
        when(create.isSetItem()).thenReturn(true);
        when(create.getItem()).thenReturn(item);
        when(item.isSetItemId()).thenReturn(true);
        when(item.getItemId()).thenReturn(itemId);
        when(itemId.getId()).thenReturn(id);
        return create;
    }

    protected void defaultMockFindFolders() throws ServiceCallException, HttpErrorException
    {
        FolderType folderType = mock(FolderType.class);
        FolderIdType folderIdType = mock(FolderIdType.class);
        when(folderType.isSetFolderId()).thenReturn(true);
        when(folderType.getFolderId()).thenReturn(folderIdType);
        when(folderIdType.getId()).thenReturn(DEFAULT_FOLDER_ID);
        mockFindFolders(new FolderType[]{folderType});
    }

    protected void mockFindFolders(final FolderType[] folders) throws ServiceCallException, HttpErrorException
    {
        mockFindFolders(folders, defaultUser);
    }

    protected void mockFindFolders(final FolderType[] folders, final String user)
            throws ServiceCallException, HttpErrorException
    {
        FindFolderType findFolder =
                FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT);
        FindFolderResponseType findFolderResponse = mock(FindFolderResponseType.class);
        ArrayOfResponseMessagesType findFolderArrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        FindFolderResponseMessageType findFolderResponseMessage = mock(FindFolderResponseMessageType.class);
        FindFolderParentType findFolderParent = mock(FindFolderParentType.class);
        ArrayOfFoldersType arrayOfFolders = mock(ArrayOfFoldersType.class);
        when(findFolderParent.getFolders()).thenReturn(arrayOfFolders);
        when(service.findFolder(likeThis(findFolder), eq(user))).thenReturn(findFolderResponse);
        when(findFolderResponse.getResponseMessages()).thenReturn(findFolderArrayOfResponseMessages);
        when(findFolderArrayOfResponseMessages.getFindFolderResponseMessageArray())
                .thenReturn(new FindFolderResponseMessageType[]{findFolderResponseMessage});
        when(findFolderResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findFolderResponseMessage.isSetRootFolder()).thenReturn(true);
        when(findFolderResponseMessage.getRootFolder()).thenReturn(findFolderParent);
        when(findFolderParent.isSetFolders()).thenReturn(true);
        when(findFolderParent.getFolders()).thenReturn(arrayOfFolders);
        when(arrayOfFolders.getFolderArray()).thenReturn(folders);
    }

    protected MessageType mockMessageItemId(final String itemId)
    {
        MessageType mockedMessage = mock(MessageType.class);
        ItemIdType mockedId = mock(ItemIdType.class);
        when(mockedMessage.isSetItemId()).thenReturn(true);
        when(mockedMessage.getItemId()).thenReturn(mockedId);
        when(mockedId.getId()).thenReturn(itemId);
        return mockedMessage;
    }

    protected void mockGetItem(final MessageType[] findResults, final int initialOffset, final int pageSize,
                               final int pageIndex, final int max, final String folder)
            throws XmlException, ServiceCallException, IOException, HttpErrorException
    {
        mockGetItem(findResults, initialOffset, pageSize, pageIndex, max, folder, defaultUser);
    }

    protected void mockGetItem(final MessageType[] findResults, final int initialOffset, final int pageSize,
                               final int pageIndex, final int max, final String folder, final String user)
            throws XmlException, ServiceCallException, IOException, HttpErrorException
    {
        int start = pageSize * pageIndex;
        int trueMax = Math.min(max, start + pageSize);
        mockGetItem(Arrays.copyOfRange(findResults, start, trueMax),
                    generateIds(initialOffset + start, trueMax - start, folder), user);
    }

    protected void mockGetItem(final MessageType[] messages, final List<String> requestedList)
            throws XmlException, ServiceCallException, IOException, HttpErrorException
    {
        mockGetItem(messages, requestedList, defaultUser);
    }

    protected void mockGetItem(final MessageType[] messages, final List<String> requestedList, final String user)
            throws XmlException, ServiceCallException, IOException, HttpErrorException
    {
        GetItemType getItem = GetItemHelper.getGetItemsRequest(requestedList);
        GetItemResponseType getItemResponse = mock(GetItemResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        ItemInfoResponseMessageType itemInfoResponseMessage = mock(ItemInfoResponseMessageType.class);
        ArrayOfRealItemsType arrayOfRealItems = mock(ArrayOfRealItemsType.class);
        when(service.getItem(likeThis(getItem), eq(user))).thenReturn(getItemResponse);
        when(getItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getGetItemResponseMessageArray())
                .thenReturn(new ItemInfoResponseMessageType[]{itemInfoResponseMessage});
        when(itemInfoResponseMessage.getItems()).thenReturn(arrayOfRealItems);
        when(arrayOfRealItems.getMessageArray()).thenReturn(messages);
    }
}
