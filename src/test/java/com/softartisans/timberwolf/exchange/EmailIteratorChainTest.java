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

import java.util.regex.Pattern;

public class EmailIteratorChainTest
{
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
        FindItemResponseType response = mock(FindItemResponseType.class);
        ArrayOfResponseMessagesType msgArr = mock(ArrayOfResponseMessagesType.class);
        when(msgArr.getFindItemResponseMessageArray()).thenReturn(new FindItemResponseMessageType[] { });
        when(response.getResponseMessages()).thenReturn(msgArr);
        when(service.findItem(any(FindItemType.class), eq("bkerr@INT.TARTARUS.COM"))).thenReturn(response);

        ArrayList<String> users = new ArrayList<String>();
        users.add("bkerr@INT.TARTARUS.COM");
        EmailIteratorChain chain = new EmailIteratorChain(service, users);

        assertFalse(chain.hasNext());
        assertNull(chain.next());
    }
}
