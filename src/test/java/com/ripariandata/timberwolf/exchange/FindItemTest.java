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
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.BasePathToElementType;
import com.microsoft.schemas.exchange.services.x2006.types.ConstantValueType;
import com.microsoft.schemas.exchange.services.x2006.types.FieldURIOrConstantType;
import com.microsoft.schemas.exchange.services.x2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services.x2006.types.IsGreaterThanType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.PathToUnindexedFieldType;
import com.microsoft.schemas.exchange.services.x2006.types.RestrictionType;
import com.microsoft.schemas.exchange.services.x2006.types.SearchExpressionType;
import java.util.Vector;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static com.ripariandata.timberwolf.exchange.IsXmlBeansRequest.likeThis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for all the FindItem specific stuff.
 */
public class FindItemTest extends ExchangeTestBase
{
    private final String id = "A super unique folder id";
    private FolderContext folderContext = new FolderContext(id, getDefaultUser());

    private static final int DEFAULT_MAX_ENTRIES = 1000;

    @Test
    public void testGetFindItemsRequestOffset() throws ServiceCallException
    {
        final int maxEntries = 10;
        Configuration config = new Configuration(maxEntries, 0);

        final int offset = 3;
        FindItemType request = FindItemHelper.getFindItemsRequest(config, folderContext, offset);
        assertEquals(offset, request.getIndexedPageItemView().getOffset());

        final int unusualOffset = 13;
        request = FindItemHelper.getFindItemsRequest(config, folderContext, unusualOffset);
        assertEquals(unusualOffset, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, folderContext, 0);
        assertEquals(0, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, folderContext, -1);
        assertEquals(0, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, folderContext, 1);
        assertEquals(1, request.getIndexedPageItemView().getOffset());
    }

    private void assertFindItemsRequestMaxEntries(final int maxItems) throws ServiceCallException
    {
        Configuration config = new Configuration(maxItems, 0);
        final int offset = 5;
        FindItemType request = FindItemHelper.getFindItemsRequest(config, folderContext, offset);
        assertEquals(Math.max(1, maxItems), request.getIndexedPageItemView().getMaxEntriesReturned());
    }

    @Test
    public void testGetFindItemsRequestMaxEntries() throws ServiceCallException
    {
        final int maxEntries1 = 10;
        assertFindItemsRequestMaxEntries(maxEntries1);
        final int maxEntries2 = 3;
        assertFindItemsRequestMaxEntries(maxEntries2);
        final int maxEntries3 = 0;
        assertFindItemsRequestMaxEntries(maxEntries3);
        final int maxEntries4 = 1;
        assertFindItemsRequestMaxEntries(maxEntries4);
    }

    @Test
    public void testFindItemsInboxRespondNull()
            throws ServiceCallException, HttpErrorException
    {
        Configuration config = new Configuration(DEFAULT_MAX_ENTRIES, 0);
        FindItemType findItem = FindItemHelper.getFindItemsRequest(config, folderContext, 0);
        when(getService().findItem(likeThis(findItem), eq(getDefaultUser()))).thenReturn(null);

        try
        {
            Vector<String> items = FindItemHelper.findItems(getService(), config, folderContext, 0);
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
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(getService(), getDefaultConfig(), getDefaultFolder(), 0);
        assertEquals(0, items.size());
    }

    @Test
    public void testFindItemsItemsRespond1()
            throws ServiceCallException, HttpErrorException
    {
        MessageType message = mockMessageItemId("foobar27");
        MessageType[] messages = new MessageType[]{message};
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(getService(), getDefaultConfig(), getDefaultFolder(), 0);
        Vector<String> expected = new Vector<String>(1);
        expected.add("foobar27");
        assertEquals(expected, items);
    }

    @Test
    public void testFindItemsItemsRespond100()
            throws ServiceCallException, HttpErrorException
    {
        final int count = 100;
        MessageType[] messages = new MessageType[count];
        for (int i = 0; i < count; i++)
        {
            messages[i] = mockMessageItemId("the" + i + "id");
        }
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(getService(), getDefaultConfig(), getDefaultFolder(), 0);
        Vector<String> expected = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            expected.add("the" + i + "id");
        }
        assertEquals(expected, items);
    }

    @Test
    public void testFindItemsItemsMissingId()
            throws ServiceCallException, HttpErrorException
    {
        final int count = 3;
        int unset = 1;
        MessageType[] messages = new MessageType[count];
        for (int i = 0; i < count; i++)
        {
            messages[i] = mockMessageItemId("the" + i + "id");
        }

        messages[unset] = mock(MessageType.class);
        when(messages[unset].isSetItemId()).thenReturn(false);
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(getService(), getDefaultConfig(), getDefaultFolder(), 0);
        Vector<String> expected = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            expected.add("the" + i + "id");
        }
        expected.remove(1);
        assertEquals(expected, items);
    }

    @Test
    public void testFindItemsWithErrorResponse()
            throws ServiceCallException, HttpErrorException
    {
        FindItemResponseMessageType findMessage = mock(FindItemResponseMessageType.class);
        when(findMessage.getResponseCode()).thenReturn(ResponseCodeType.ERROR_ACCESS_DENIED);
        ArrayOfResponseMessagesType responseArr = mock(ArrayOfResponseMessagesType.class);
        when(responseArr.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{findMessage});
        FindItemResponseType findResponse = mock(FindItemResponseType.class);
        when(findResponse.getResponseMessages()).thenReturn(responseArr);
        when(getService().findItem(any(FindItemType.class), eq(getDefaultUser()))).thenReturn(findResponse);

        try
        {
            Configuration config = new Configuration(DEFAULT_MAX_ENTRIES, 0);
            FindItemHelper.findItems(getService(), config, folderContext, 0);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response contained an error.", e.getMessage());
        }
    }

    @Test
    public void testFindItemsNoRootFolder() throws ServiceCallException, HttpErrorException
    {
        FindItemResponseType findItemResponse = mock(FindItemResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        FindItemResponseMessageType findItemResponseMessage = mock(FindItemResponseMessageType.class);
        FindItemParentType findItemParent = mock(FindItemParentType.class);
        FindItemType findItem = FindItemHelper.getFindItemsRequest(getDefaultConfig(), getDefaultFolder(), 0);
        when(getService().findItem(likeThis(findItem), eq(getDefaultUser()))).thenReturn(findItemResponse);
        when(findItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{findItemResponseMessage});
        when(findItemResponseMessage.getRootFolder()).thenReturn(findItemParent);
        when(findItemResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findItemResponseMessage.isSetRootFolder()).thenReturn(false);
    }

    @Test
    public void testFindItemsNoId() throws ServiceCallException, HttpErrorException
    {
        FindItemResponseType findItemResponse = mock(FindItemResponseType.class);
        ArrayOfResponseMessagesType arrayOfResponseMessages = mock(ArrayOfResponseMessagesType.class);
        FindItemResponseMessageType findItemResponseMessage = mock(FindItemResponseMessageType.class);
        FindItemParentType findItemParent = mock(FindItemParentType.class);
        FindItemType findItem = FindItemHelper.getFindItemsRequest(getDefaultConfig(), getDefaultFolder(), 0);
        when(getService().findItem(likeThis(findItem), eq(getDefaultUser()))).thenReturn(findItemResponse);
        when(findItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{findItemResponseMessage});
        when(findItemResponseMessage.getRootFolder()).thenReturn(findItemParent);
        when(findItemResponseMessage.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        when(findItemResponseMessage.isSetRootFolder()).thenReturn(false);
    }

    @Test
    public void testGetAfterDateRestriction() throws ServiceCallException
    {
        RestrictionType restriction = FindItemHelper.getAfterDateRestriction(new DateTime(0));
        SearchExpressionType searchExpression = restriction.getSearchExpression();
        assertTrue("Restriction was not using a greater than comparison.",
                   searchExpression instanceof IsGreaterThanType);
        IsGreaterThanType greaterThan = (IsGreaterThanType) searchExpression;
        BasePathToElementType basePath = greaterThan.getPath();
        assertTrue("Restriction was not using an unindexed field uri.", basePath instanceof PathToUnindexedFieldType);
        PathToUnindexedFieldType fieldUri = (PathToUnindexedFieldType) basePath;
        assertEquals("item:DateTimeReceived", fieldUri.getFieldURI().toString());
        FieldURIOrConstantType startDate = greaterThan.getFieldURIOrConstant();
        assertFalse("Restriction was comparing against a path, not a constant.", startDate.isSetPath());
        assertTrue("Resitriction was not comparing against a constant.", startDate.isSetConstant());
        ConstantValueType constant = startDate.getConstant();
        assertEquals("Epoch time 0 did not produce the correct string.", "1970-01-01T00:00:00", constant.getValue());

        final int year = 2000;
        final int month = 2;
        final int day = 10;
        final int hour = 3;
        final int minute = 4;
        final int seconds = 55;
        final int milliseconds = 0;
        final int tzOffset = -5;
        final int offsetDay = 9;
        final int offsetHour = 22;

        restriction = FindItemHelper.getAfterDateRestriction(
            new DateTime(year, month, day, hour, minute, seconds, milliseconds, DateTimeZone.UTC));
        String date = ((IsGreaterThanType) restriction.getSearchExpression()).getFieldURIOrConstant()
                                                     .getConstant().getValue();
        assertEquals("UTC date time did not produce the correct string.", "2000-02-10T03:04:55", date);

        restriction = FindItemHelper.getAfterDateRestriction(
            new DateTime(year, month, offsetDay, offsetHour, minute, seconds, milliseconds,
                         DateTimeZone.forOffsetHours(tzOffset)));
        date = ((IsGreaterThanType) restriction.getSearchExpression()).getFieldURIOrConstant().getConstant().getValue();
        assertEquals("Non-UTC date time did not produce the correct string.", "2000-02-10T03:04:55", date);
    }
}
