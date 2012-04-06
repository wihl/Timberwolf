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
package com.ripariandata.timberwolf.mail.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.FindFolderParentType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FolderType;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Vector;

import org.junit.Test;

import static com.ripariandata.timberwolf.mail.exchange.IsXmlBeansRequest.likeThis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** A test fixture for the FindFolder specific tests. */
public class FindFolderTest extends ExchangeTestBase
{

    private FolderType mockFolderType(final String folderId)
    {
        FolderType folder = mock(FolderType.class);
        FolderIdType folderIdHolder = mock(FolderIdType.class);
        when(folder.isSetFolderId()).thenReturn(true);
        when(folder.getFolderId()).thenReturn(folderIdHolder);
        when(folderIdHolder.getId()).thenReturn(folderId);
        return folder;
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
    public void testFindFolders() throws ServiceCallException, HttpErrorException
    {
        final int count = 10;

        List<String> ids = new ArrayList<String>(count);
        FolderType[] folders = new FolderType[count];

        for (int i = 0; i < count; i++)
        {
            String id = "SADG345GFGFEFHGGFH454fgH56FDDGFNGGERTTGH%$466" + i;
            ids.add(id);
            folders[i] = mockFolderType(id);
        }
        mockFindFolders(folders);

        FindFolderType findFoldersRequest = FindFolderHelper.getFindFoldersRequest(
                DistinguishedFolderIdNameType.MSGFOLDERROOT);
        Queue<String> foldersVec = FindFolderHelper.findFolders(getService(), findFoldersRequest, getDefaultUser());
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
        when(getService().findFolder(likeThis(findFolder), eq(getDefaultUser()))).thenReturn(findFolderResponse);
        when(findFolderResponse.getResponseMessages()).thenReturn(findFolderArrayOfResponseMessages);
        when(findFolderArrayOfResponseMessages.getFindFolderResponseMessageArray())
                .thenReturn(new FindFolderResponseMessageType[]{findFolderResponseMessage});
        when(findFolderResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findFolderResponseMessage.isSetRootFolder()).thenReturn(false);
        FindFolderHelper.findFolders(getService(), findFolder, getDefaultUser());
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
        when(getService().findFolder(likeThis(findFolder), eq(getDefaultUser()))).thenReturn(findFolderResponse);
        when(findFolderResponse.getResponseMessages()).thenReturn(findFolderArrayOfResponseMessages);
        when(findFolderArrayOfResponseMessages.getFindFolderResponseMessageArray())
                .thenReturn(new FindFolderResponseMessageType[]{findFolderResponseMessage});
        when(findFolderResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findFolderResponseMessage.isSetRootFolder()).thenReturn(true);
        when(findFolderResponseMessage.getRootFolder()).thenReturn(findFolderParent);
        when(findFolderParent.isSetFolders()).thenReturn(false);
        FindFolderHelper.findFolders(getService(), findFolder, getDefaultUser());
    }

    @Test
    public void testFindFoldersNoFolderId() throws ServiceCallException, HttpErrorException
    {
        final int count = 3;
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
                getService(), FindFolderHelper.getFindFoldersRequest(DistinguishedFolderIdNameType.MSGFOLDERROOT),
                getDefaultUser());
        Vector<String> expected = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            expected.add("the" + i + "id");
        }
        expected.remove(1);
        assertEquals(expected, items);
    }
}
