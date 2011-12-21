package com.softartisans.timberwolf.exchange;

import com.cloudera.alfredo.client.AuthenticationException;
import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfRealItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import org.apache.xmlbeans.XmlException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import static com.softartisans.timberwolf.exchange.IsXmlBeansRequest.LikeThis;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for ExchangeMailStore, uses mock exchange service
 */
public class ExchangeMailStoreTest
{
    @Mock
    private FindItemResponseType findItemResponse;
    @Mock
    private ArrayOfResponseMessagesType arrayOfResponseMessages;
    @Mock
    private FindItemResponseMessageType findItemResponseMessage;
    @Mock
    private FindItemParentType findItemParent;
    @Mock
    private ArrayOfRealItemsType arrayOfRealItems;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception
    {

    }

    @Test
    public void testGetFindItemsRequestInbox()
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId =
                findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.INBOX);
        assertEquals(findItem.xmlText(),
                     ExchangeMailStore.getFindItemsRequest(
                             DistinguishedFolderIdNameType.INBOX).xmlText());
    }

    @Test
    public void testGetFindItemsRequestDeletedItems()
    {
        FindItemType findItem = FindItemType.Factory.newInstance();
        findItem.setTraversal(ItemQueryTraversalType.SHALLOW);
        findItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ID_ONLY);
        DistinguishedFolderIdType folderId =
                findItem.addNewParentFolderIds().addNewDistinguishedFolderId();
        folderId.setId(DistinguishedFolderIdNameType.DELETEDITEMS);
        assertEquals(findItem.xmlText(),
                     ExchangeMailStore.getFindItemsRequest(
                             DistinguishedFolderIdNameType.DELETEDITEMS).xmlText());
    }

    @Test
    @Ignore("HAM-33 - I really have to stop writing negative tests")
    public void testFindItemsInboxRespondNull()
            throws XmlException, IOException,
                   HttpUrlConnectionCreationException, AuthenticationException
    {
        ExchangeService service = mock(ExchangeService.class);
        FindItemType findItem = ExchangeMailStore
                .getFindItemsRequest(DistinguishedFolderIdNameType.INBOX);
        when(service.findItem(LikeThis(findItem))).thenReturn(null);
        Vector<String> items = ExchangeMailStore.findItems(service);
        assertEquals(0,items.size());
    }

    @Test
    public void testFindItemsItemsRespond0()
            throws XmlException, IOException,
                   HttpUrlConnectionCreationException, AuthenticationException
    {
        ExchangeService service = mock(ExchangeService.class);
        FindItemType findItem = ExchangeMailStore
                .getFindItemsRequest(
                        DistinguishedFolderIdNameType.INBOX);
        when(service.findItem(LikeThis(findItem))).thenReturn(findItemResponse);
        when(findItemResponse.getResponseMessages()).thenReturn(
                arrayOfResponseMessages);
        when(arrayOfResponseMessages.getFindItemResponseMessageArray())
                .thenReturn(new FindItemResponseMessageType[]{
                        findItemResponseMessage});
        when(findItemResponseMessage.getRootFolder()).thenReturn(findItemParent);
        // For logging right now, might actually be checked later
        when(findItemResponseMessage.getResponseCode()).thenReturn(
                ResponseCodeType.NO_ERROR);
        when(findItemParent.getItems()).thenReturn(arrayOfRealItems);
        when(arrayOfRealItems.getMessageArray()).thenReturn(new MessageType[0]);
        Vector<String> items = ExchangeMailStore.findItems(service);
        assertEquals(0, items.size());
    }

    @Test
    @Ignore("HAM-33 - I'm not sure what the exchange response here would be "
            + "but I can't get it because ExchangeService doesn't handle it.")
    public void testGetGetItemsRequestNull()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(),
                     ExchangeMailStore
                             .getGetItemsRequest(null)
                             .xmlText());
    }

    @Test
    @Ignore("HAM-33 - I'm not sure what the exchange response here would be "
            + "but I can't get it because ExchangeService doesn't handle it.")
    public void testGetGetItemsRequest0()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(),
                     ExchangeMailStore
                             .getGetItemsRequest(new ArrayList<String>())
                             .xmlText());
    }

    @Test
    public void testGetGetItemsRequest1()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        items.addNewItemId().setId("idNumber0");
        ArrayList<String> ids = new ArrayList<String>();
        ids.add("idNumber0");
        assertEquals(getItem.xmlText(),
                     ExchangeMailStore.getGetItemsRequest(ids).xmlText());
    }

    @Test
    public void testGetGetItemsRequest100()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        for (int i=0; i<100; i++)
        {
            items.addNewItemId().setId("idNumber" + i);
        }
        ArrayList<String> ids = new ArrayList<String>();
        for (int i=0; i<100; i++)
        {
            ids.add("idNumber" + i);
        }
        assertEquals(getItem.xmlText(),
                     ExchangeMailStore.getGetItemsRequest(ids).xmlText());
    }

}
