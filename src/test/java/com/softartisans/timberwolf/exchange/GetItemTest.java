package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import com.softartisans.timberwolf.MailboxItem;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.xmlbeans.XmlException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test fixture for the tests specific to GetItem.
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
        final int count = 100;
        for (int i = 0; i < count; i++)
        {
            items.addNewItemId().setId("idNumber" + i);
        }
        ArrayList<String> ids = new ArrayList<String>();
        for (int i = 0; i < count; i++)
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
        Vector<MailboxItem> items = GetItemHelper.getItems(0, 0, list, service);
        assertEquals(0, items.size());
    }

    @Test
    public void testGetItems0to1()
            throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        final int count = 5;
        Vector<String> wholeList = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        wholeList.set(0, idValue);
        requestedList.add(idValue);
        mockGetItem(new MessageType[]{mockMessageItemId(idValue)}, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, 0, wholeList, service);
        assertEquals(1, items.size());
        assertEquals(idValue, items.get(0).getHeader(idHeaderKey));
    }

    @Test
    public void testGetItems3to4()
            throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        final int count = 5;
        Vector<String> wholeList = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        final int wholeListCount = 3;
        wholeList.set(wholeListCount, idValue);
        requestedList.add(idValue);
        mockGetItem(new MessageType[]{mockMessageItemId(idValue)}, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, wholeListCount, wholeList, service);
        assertEquals(1, items.size());
        assertEquals(idValue, items.get(0).getHeader(idHeaderKey));
    }

    @Test
    public void testGetItems2to3Return0()
            throws XmlException, IOException, HttpErrorException,
                   ServiceCallException
    {
        final int count = 5;
        Vector<String> wholeList = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        String idValue = "id1";
        final int wholeListCount = 3;
        wholeList.set(wholeListCount, idValue);
        requestedList.add(idValue);
        mockGetItem(new MessageType[0], requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(1, wholeListCount, wholeList, service);
        assertEquals(0, items.size());
    }

    @Test
    public void testGetItems2to93()
            throws ServiceCallException, HttpErrorException, XmlException, IOException
    {
        final int count = 100;
        Vector<String> wholeList = new Vector<String>(count);
        for (int i = 0; i < count; i++)
        {
            wholeList.add(null);
        }
        List<String> requestedList = new Vector<String>(1);
        final int messageTypeCount = 91;
        MessageType[] messages = new MessageType[messageTypeCount];
        final int messageLoopCount = 93;
        for (int i = 2; i < messageLoopCount; i++)
        {
            String id = "id #" + i;
            wholeList.set(i, id);
            requestedList.add(id);
            messages[i - 2] = mockMessageItemId(id);
        }
        mockGetItem(messages, requestedList);
        Vector<MailboxItem> items = GetItemHelper.getItems(messageTypeCount, 2, wholeList, service);
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
        when(service.getItem(any(GetItemType.class))).thenReturn(getResponse);

        Vector<String> ids = new Vector<String>();
        ids.add("abcd");

        try
        {
            GetItemHelper.getItems(1, 0, ids, service);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response contained an error.", e.getMessage());
        }
    }

}
