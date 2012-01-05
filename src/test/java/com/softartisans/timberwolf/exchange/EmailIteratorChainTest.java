package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.types.FindItemParentType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;

import static org.junit.Assert.*;
import org.junit.Test;

import static org.mockito.Mockito.*;

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
}
