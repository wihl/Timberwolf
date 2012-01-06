package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfRealItemsType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import com.microsoft.schemas.exchange.services.x2006.types.SingleRecipientType;
import com.microsoft.schemas.exchange.services.x2006.types.EmailAddressType;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;

import static org.junit.Assert.*;
import org.junit.Test;

import static com.softartisans.timberwolf.exchange.IsXmlBeansRequest.LikeThis;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.io.UnsupportedEncodingException;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.xmlbeans.XmlException;

import com.softartisans.timberwolf.MailboxItem;

import java.util.regex.Pattern;

public class EmailIteratorChainTest
{
    private void addEmptyUserToMockService(ExchangeService mockService, String user)
        throws ServiceCallException, HttpErrorException
    {
        FindItemResponseType response = mock(FindItemResponseType.class);
        ArrayOfResponseMessagesType msgArr = mock(ArrayOfResponseMessagesType.class);
        when(msgArr.getFindItemResponseMessageArray()).thenReturn(new FindItemResponseMessageType[] { });
        when(response.getResponseMessages()).thenReturn(msgArr);
        when(mockService.findItem(any(FindItemType.class), eq(user))).thenReturn(response);
    }

    private void addFullUserToMockService(ExchangeService mockService, String user, int messageCount)
        throws ServiceCallException, HttpErrorException
    {
        FindItemResponseType response = mock(FindItemResponseType.class);
        ArrayOfResponseMessagesType msgArr = mock(ArrayOfResponseMessagesType.class);
        FindItemResponseMessageType findMsg = mock(FindItemResponseMessageType.class);
        when(findMsg.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        FindItemParentType rootFolder = mock(FindItemParentType.class);
        ArrayOfRealItemsType items = mock(ArrayOfRealItemsType.class);

        MessageType[] findMsgs = new MessageType[messageCount];
        for (int i = 0; i < messageCount; i++)
        {
            MessageType foundMsg = mock(MessageType.class);
            ItemIdType findId = mock(ItemIdType.class);
            when(findId.getId()).thenReturn("item " + i);
            when(foundMsg.getItemId()).thenReturn(findId);
            findMsgs[i] = foundMsg;
        }

        when(items.getMessageArray()).thenReturn(findMsgs);
        when(rootFolder.getItems()).thenReturn(items);
        when(findMsg.getRootFolder()).thenReturn(rootFolder);;
        when(msgArr.getFindItemResponseMessageArray()).thenReturn(new FindItemResponseMessageType[] { findMsg });
        when(response.getResponseMessages()).thenReturn(msgArr);
        when(mockService.findItem(any(FindItemType.class), eq(user))).thenReturn(response);

        GetItemResponseType getResponse = mock(GetItemResponseType.class);
        ArrayOfResponseMessagesType getMsgArr = mock(ArrayOfResponseMessagesType.class);
        ItemInfoResponseMessageType getMsg = mock(ItemInfoResponseMessageType.class);
        when(getMsg.getResponseCode()).thenReturn(ResponseCodeType.NO_ERROR);
        ArrayOfRealItemsType getItems = mock(ArrayOfRealItemsType.class);

        MessageType[] getMsgs = new MessageType[messageCount];
        for (int i = 0; i < messageCount; i++)
        {
            MessageType gotMsg = mock(MessageType.class);
            when(gotMsg.isSetItemId()).thenReturn(true);
            ItemIdType gotId = mock(ItemIdType.class);
            when(gotId.getId()).thenReturn("item " + i);
            when(gotMsg.getItemId()).thenReturn(gotId);
            when(gotMsg.isSetFrom()).thenReturn(true);
            SingleRecipientType from = mock(SingleRecipientType.class);
            EmailAddressType address = mock(EmailAddressType.class);
            when(address.isSetEmailAddress()).thenReturn(true);
            when(address.getEmailAddress()).thenReturn(user);
            when(from.getMailbox()).thenReturn(address);
            when(gotMsg.getFrom()).thenReturn(from);
            getMsgs[i] = gotMsg;
        }

        when(getItems.getMessageArray()).thenReturn(getMsgs);
        when(getMsg.getItems()).thenReturn(getItems);
        when(getMsgArr.getGetItemResponseMessageArray()).thenReturn(new ItemInfoResponseMessageType[] { getMsg });
        when(getResponse.getResponseMessages()).thenReturn(getMsgArr);
        when(mockService.getItem(any(GetItemType.class), eq(user))).thenReturn(getResponse);
    }

    @Test
    public void testChainNoUsers()
    {
        ExchangeService service = mock(ExchangeService.class);
        ArrayList<String> users = new ArrayList<String>();
        EmailIteratorChain chain = new EmailIteratorChain(service, users);

        assertFalse(chain.hasNext());
        assertNull(chain.next());
    }

    @Test
    public void testChainOneEmptyUser() throws ServiceCallException, HttpErrorException
    {
        ExchangeService service = mock(ExchangeService.class);
        addEmptyUserToMockService(service, "bkerr@INT.TARTARUS.COM");

        ArrayList<String> users = new ArrayList<String>();
        users.add("bkerr@INT.TARTARUS.COM");
        EmailIteratorChain chain = new EmailIteratorChain(service, users);

        assertFalse(chain.hasNext());
        assertNull(chain.next());
    }

    @Test
    public void testChainManyEmptyUsers() throws ServiceCallException, HttpErrorException
    {
        ExchangeService service = mock(ExchangeService.class);
        addEmptyUserToMockService(service, "bkerr@INT.TARTARUS.COM");
        addEmptyUserToMockService(service, "abenjamin@INT.TARTARUS.COM");
        addEmptyUserToMockService(service, "dkramer@INT.TARTARUS.COM");
        addEmptyUserToMockService(service, "wgill@INT.TARTARUS.COM");

        ArrayList<String> users = new ArrayList<String>();
        users.add("abenjamin@INT.TARTARUS.COM");
        users.add("dkramer@INT.TARTARUS.COM");
        users.add("bkerr@INT.TARTARUS.COM");
        users.add("wgill@INT.TARTARUS.COM");
        EmailIteratorChain chain = new EmailIteratorChain(service, users);

        assertFalse(chain.hasNext());
        assertNull(chain.next());
    }

    @Test
    public void testChainOneFullUser() throws ServiceCallException, HttpErrorException
    {
        ExchangeService service = mock(ExchangeService.class);
        addFullUserToMockService(service, "bkerr@INT.TARTARUS.COM", 1);

        ArrayList<String> users = new ArrayList<String>();
        users.add("bkerr@INT.TARTARUS.COM");
        EmailIteratorChain chain = new EmailIteratorChain(service, users);

        assertTrue(chain.hasNext());
        MailboxItem item = chain.next();
        assertEquals("item 0", item.getHeader("Item ID"));
        assertEquals("bkerr@INT.TARTARUS.COM", item.getHeader("Sender"));
        assertFalse(chain.hasNext());
        assertNull(chain.next());
    }

    @Test
    public void testChainManyFullUsers() throws ServiceCallException, HttpErrorException
    {
        ExchangeService service = mock(ExchangeService.class);
        addFullUserToMockService(service, "bkerr@INT.TARTARUS.COM", 1);
        addFullUserToMockService(service, "abenjamin@INT.TARTARUS.COM", 2);
        addFullUserToMockService(service, "dkramer@INT.TARTARUS.COM", 3);
        addFullUserToMockService(service, "wgill@INT.TARTARUS.COM", 1);

        ArrayList<String> users = new ArrayList<String>();
        users.add("abenjamin@INT.TARTARUS.COM");
        users.add("bkerr@INT.TARTARUS.COM");
        users.add("wgill@INT.TARTARUS.COM");
        users.add("dkramer@INT.TARTARUS.COM");
        EmailIteratorChain chain = new EmailIteratorChain(service, users);

        assertTrue(chain.hasNext());
        MailboxItem item = chain.next();
        assertEquals("item 0", item.getHeader("Item ID"));
        assertEquals("abenjamin@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 1", item.getHeader("Item ID"));
        assertEquals("abenjamin@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 0", item.getHeader("Item ID"));
        assertEquals("bkerr@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 0", item.getHeader("Item ID"));
        assertEquals("wgill@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 0", item.getHeader("Item ID"));
        assertEquals("dkramer@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 1", item.getHeader("Item ID"));
        assertEquals("dkramer@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 1", item.getHeader("Item ID"));
        assertEquals("dkramer@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertFalse(chain.hasNext());
        assertNull(chain.next());
    }

    @Test
    public void testChainManyMixedUsers() throws ServiceCallException, HttpErrorException
    {
        ExchangeService service = mock(ExchangeService.class);
        addEmptyUserToMockService(service, "bkerr@INT.TARTARUS.COM");
        addFullUserToMockService(service, "abenjamin@INT.TARTARUS.COM", 2);
        addFullUserToMockService(service, "dkramer@INT.TARTARUS.COM", 3);
        addEmptyUserToMockService(service, "wgill@INT.TARTARUS.COM");
        addFullUserToMockService(service, "mprince@INT.TARTARUS.COM", 1);
        addEmptyUserToMockService(service, "korganizer@INT.TARTARUS.COM");
        addEmptyUserToMockService(service, "tsender@INT.TARTARUS.COM");

        ArrayList<String> users = new ArrayList<String>();
        users.add("abenjamin@INT.TARTARUS.COM");
        users.add("bkerr@INT.TARTARUS.COM");
        users.add("dkramer@INT.TARTARUS.COM");
        users.add("korganizer@INT.TARTARUS.COM");
        users.add("mprince@INT.TARTARUS.COM");
        users.add("tsender@INT.TARTARUS.COM");
        users.add("wgill@INT.TARTARUS.COM");
        EmailIteratorChain chain = new EmailIteratorChain(service, users);

        assertTrue(chain.hasNext());
        MailboxItem item = chain.next();
        assertEquals("item 0", item.getHeader("Item ID"));
        assertEquals("abenjamin@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 1", item.getHeader("Item ID"));
        assertEquals("abenjamin@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 0", item.getHeader("Item ID"));
        assertEquals("dkramer@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 1", item.getHeader("Item ID"));
        assertEquals("dkramer@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 2", item.getHeader("Item ID"));
        assertEquals("dkramer@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertTrue(chain.hasNext());
        item = chain.next();
        assertEquals("item 0", item.getHeader("Item ID"));
        assertEquals("mprince@INT.TARTARUS.COM", item.getHeader("Sender"));

        assertFalse(chain.hasNext());
        assertNull(chain.next());
    }
}
