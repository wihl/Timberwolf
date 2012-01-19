package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.BasePathToElementType;
import com.microsoft.schemas.exchange.services.x2006.types.ConstantValueType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FieldURIOrConstantType;
import com.microsoft.schemas.exchange.services.x2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexBasePointType;
import com.microsoft.schemas.exchange.services.x2006.types.IndexedPageViewType;
import com.microsoft.schemas.exchange.services.x2006.types.IsGreaterThanType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.PathToUnindexedFieldType;
import com.microsoft.schemas.exchange.services.x2006.types.RestrictionType;
import com.microsoft.schemas.exchange.services.x2006.types.SearchExpressionType;
import static com.softartisans.timberwolf.exchange.IsXmlBeansRequest.LikeThis;
import java.util.Vector;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for all the FindItem specific stuff
 */
public class FindItemTest extends ExchangeTestBase
{
    private FolderContext inbox =
            new FolderContext(DistinguishedFolderIdNameType.INBOX, defaultUser);

    @Test
    public void testGetFindItemsRequestInbox() throws ServiceCallException
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId = findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.INBOX);
        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        index.setMaxEntriesReturned(1000);
        index.setBasePoint(IndexBasePointType.BEGINNING);
        index.setOffset(0);
        findItem.setRestriction(FindItemHelper.getAfterDateRestriction(new DateTime(0)));
        Configuration config = new Configuration(1000, 0);
        assertEquals(findItem.xmlText(),
                     FindItemHelper.getFindItemsRequest(config, inbox, 0).xmlText());
    }

    @Test
    public void testGetFindItemsRequestDeletedItems() throws ServiceCallException
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId = findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.DELETEDITEMS);
        IndexedPageViewType index = findItem.addNewIndexedPageItemView();
        index.setMaxEntriesReturned(1000);
        index.setBasePoint(IndexBasePointType.BEGINNING);
        index.setOffset(0);
        findItem.setRestriction(FindItemHelper.getAfterDateRestriction(new DateTime(0)));
        Configuration config = new Configuration(1000, 0);
        FolderContext folder = new FolderContext(DistinguishedFolderIdNameType.DELETEDITEMS, defaultUser);
        assertEquals(findItem.xmlText(),
                     FindItemHelper.getFindItemsRequest(config, folder, 0).xmlText());
    }

    @Test
    public void testGetFindItemsRequestOffset() throws ServiceCallException
    {
        Configuration config = new Configuration(10, 0);

        FindItemType request = FindItemHelper.getFindItemsRequest(config, inbox, 3);
        assertEquals(3, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, inbox, 13);
        assertEquals(13, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, inbox, 0);
        assertEquals(0, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, inbox, -1);
        assertEquals(0, request.getIndexedPageItemView().getOffset());

        request = FindItemHelper.getFindItemsRequest(config, inbox, 1);
        assertEquals(1, request.getIndexedPageItemView().getOffset());
    }

    private void assertFindItemsRequestMaxEntries(int maxItems) throws ServiceCallException
    {
        Configuration config = new Configuration(maxItems, 0);
        FindItemType request = FindItemHelper.getFindItemsRequest(config, inbox, 5);
        assertEquals(Math.max(1, maxItems), request.getIndexedPageItemView().getMaxEntriesReturned());
    }

    @Test
    public void testGetFindItemsRequestMaxEntries() throws ServiceCallException
    {
        assertFindItemsRequestMaxEntries(10);
        assertFindItemsRequestMaxEntries(3);
        assertFindItemsRequestMaxEntries(0);
        assertFindItemsRequestMaxEntries(1);
    }

    @Test
    public void testFindItemsInboxRespondNull()
            throws ServiceCallException, HttpErrorException
    {
        Configuration config = new Configuration(1000, 0);
        FindItemType findItem = FindItemHelper.getFindItemsRequest(config, inbox, 0);
        when(service.findItem(LikeThis(findItem), eq(defaultUser))).thenReturn(null);

        try
        {
            Vector<String> items = FindItemHelper.findItems(service, config, inbox, 0);
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
        Vector<String> items = FindItemHelper.findItems(service, defaultConfig, defaultFolder, 0);
        assertEquals(0, items.size());
    }

    @Test
    public void testFindItemsItemsRespond1()
            throws ServiceCallException, HttpErrorException
    {
        MessageType message = mockMessageItemId("foobar27");
        MessageType[] messages = new MessageType[]{message};
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(service, defaultConfig, defaultFolder, 0);
        Vector<String> expected = new Vector<String>(1);
        expected.add("foobar27");
        assertEquals(expected, items);
    }

    @Test
    public void testFindItemsItemsRespond100()
            throws ServiceCallException, HttpErrorException
    {
        int count = 100;
        MessageType[] messages = new MessageType[count];
        for (int i = 0; i < count; i++)
        {
            messages[i] = mockMessageItemId("the" + i + "id");
        }
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(service, defaultConfig, defaultFolder, 0);
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
        int count = 3;
        int unset = 1;
        MessageType[] messages = new MessageType[count];
        for (int i = 0; i < count; i++)
        {
            messages[i] = mockMessageItemId("the" + i + "id");
        }

        messages[unset] = mock(MessageType.class);
        when(messages[unset].isSetItemId()).thenReturn(false);
        mockFindItem(messages);
        Vector<String> items = FindItemHelper.findItems(service, defaultConfig, defaultFolder, 0);
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
        when(service.findItem(any(FindItemType.class), eq(defaultUser))).thenReturn(findResponse);

        try
        {
            Configuration config = new Configuration(1000, 0);
            FindItemHelper.findItems(service, config, inbox, 0);
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
        FindItemType findItem = FindItemHelper.getFindItemsRequest(defaultConfig, defaultFolder, 0);
        when(service.findItem(LikeThis(findItem), eq(defaultUser))).thenReturn(findItemResponse);
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
        FindItemType findItem = FindItemHelper.getFindItemsRequest(defaultConfig, defaultFolder, 0);
        when(service.findItem(LikeThis(findItem), eq(defaultUser))).thenReturn(findItemResponse);
        when(findItemResponse.getResponseMessages()).thenReturn(arrayOfResponseMessages);
        when(arrayOfResponseMessages.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{ findItemResponseMessage });
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

        restriction = FindItemHelper.getAfterDateRestriction(new DateTime(2000, 2, 10, 3, 4, 55, 0, DateTimeZone.UTC));
        String date = ((IsGreaterThanType)restriction.getSearchExpression()).getFieldURIOrConstant()
                                                     .getConstant().getValue();
        assertEquals("UTC date time did not produce the correct string.", "2000-02-10T03:04:55", date);

        restriction = FindItemHelper.getAfterDateRestriction(new DateTime(2000, 2, 9, 22, 4, 55, 0,
                                                                          DateTimeZone.forOffsetHours(-5)));
        date = ((IsGreaterThanType)restriction.getSearchExpression()).getFieldURIOrConstant()
                                              .getConstant().getValue();
        assertEquals("Non-UTC date time did not produce the correct string.", "2000-02-10T03:04:55", date);
    }
}
