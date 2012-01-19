package com.ripariandata.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import com.ripariandata.timberwolf.MailboxItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import org.apache.xmlbeans.XmlException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test fixture for the tests specific to GetItem
 */
public class GetItemTest extends ExchangeTestBase
{
    private final String idHeaderKey = "Item ID";

    @Test
    public void testGetGetItemsRequestNull()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(), GetItemHelper.getGetItemsRequest(null).xmlText());
    }

    @Test
    public void testGetGetItemsRequest0()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        assertEquals(getItem.xmlText(), GetItemHelper.getGetItemsRequest(new ArrayList<String>()).xmlText());
    }

    @Test
    public void testGetGetItemsRequest1()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        items.addNewItemId().setId("idNumber0");
        ArrayList<String> ids = new ArrayList<String>();
        ids.add("idNumber0");
        assertEquals(getItem.xmlText(), GetItemHelper.getGetItemsRequest(ids).xmlText());
    }

    @Test
    public void testGetGetItemsRequest100()
    {
        GetItemType getItem = GetItemType.Factory.newInstance();
        getItem.addNewItemShape().setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        for (int i = 0; i < 100; i++)
        {
            items.addNewItemId().setId("idNumber" + i);
        }
        ArrayList<String> ids = new ArrayList<String>();
        for (int i = 0; i < 100; i++)
        {
            ids.add("idNumber" + i);
        }
        assertEquals(getItem.xmlText(), GetItemHelper.getGetItemsRequest(ids).xmlText());
    }

    @Test
    public void testGetItems0()
            throws ServiceCallException, HttpErrorException
    {
        Vector<String> list = new Vector<String>();
        Vector<MailboxItem> items = GetItemHelper.getItems(0, 0, list, service, defaultUser);
        assertEquals(0, items.size());
    }

    @Test
    public void testGetItems0to1()
            throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        Vector<String> wholeList = new Vector<String>(5);
        for (int i = 0; i < 5; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        wholeList.set(0, idValue);
        requestedList.add(idValue);
        mockGetItem(new MessageType[]{mockMessageItemId(idValue)}, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, 0, wholeList, service, defaultUser);
        assertEquals(1, items.size());
        assertEquals(idValue, items.get(0).getHeader(idHeaderKey));
    }

    @Test
    public void testGetItems3to4()
            throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        Vector<String> wholeList = new Vector<String>(5);
        for (int i = 0; i < 5; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        wholeList.set(3, idValue);
        requestedList.add(idValue);
        mockGetItem(new MessageType[]{mockMessageItemId(idValue)}, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, 3, wholeList, service, defaultUser);
        assertEquals(1, items.size());
        assertEquals(idValue, items.get(0).getHeader(idHeaderKey));
    }

    @Test
    public void testGetItems2to3Return0()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException
    {
        Vector<String> wholeList = new Vector<String>(5);
        for (int i = 0; i < 5; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        wholeList.set(3, idValue);
        requestedList.add(idValue);
        mockGetItem(new MessageType[0], requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, 3, wholeList, service, defaultUser);
        assertEquals(0, items.size());
    }

    @Test
    public void testGetItems2to93()
            throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        Vector<String> wholeList = new Vector<String>(100);
        for (int i = 0; i < 100; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        MessageType[] messages = new MessageType[91];
        for (int i = 2; i < 93; i++)
        {
            String id = "id #" + i;
            wholeList.set(i, id);
            requestedList.add(id);
            messages[i - 2] = mockMessageItemId(id);
        }
        mockGetItem(messages, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(91, 2, wholeList, service, defaultUser);
        assertEquals(requestedList.size(), items.size());
        for (int i = 0; i < requestedList.size(); i++)
        {
            assertEquals(requestedList.get(i), items.get(i).getHeader(idHeaderKey));
        }
    }

    @Test
    public void testGetItemsWithErrorResponse()
            throws ServiceCallException, HttpErrorException
    {
        ItemInfoResponseMessageType infoMessage = mock(ItemInfoResponseMessageType.class);
        when(infoMessage.getResponseCode()).thenReturn(ResponseCodeType.ERROR_ACCESS_DENIED);
        ArrayOfResponseMessagesType responseArr = mock(ArrayOfResponseMessagesType.class);
        when(responseArr.getGetItemResponseMessageArray())
                .thenReturn(new ItemInfoResponseMessageType[]{infoMessage});
        GetItemResponseType getResponse = mock(GetItemResponseType.class);
        when(getResponse.getResponseMessages()).thenReturn(responseArr);
        when(service.getItem(any(GetItemType.class), eq(defaultUser))).thenReturn(getResponse);

        Vector<String> ids = new Vector<String>();
        ids.add("abcd");

        try
        {
            GetItemHelper.getItems(1, 0, ids, service, defaultUser);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response contained an error.", e.getMessage());
        }
    }

}
