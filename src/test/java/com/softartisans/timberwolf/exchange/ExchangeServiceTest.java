package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
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
import org.apache.xmlbeans.XmlException;

import java.util.regex.Pattern;

public class ExchangeServiceTest
{
    private static final String url = "https://example.com/ews/exchange.asmx";
    private static final String soapPrelude =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>" +
        "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">" +
        "<soapenv:Body>";
    private static final String soapFinale =
        "</soapenv:Body>" +
        "</soapenv:Envelope>";
    private static final String findItemsRequest =
        "<FindItem Traversal=\"Shallow\" " +
        "xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\" " +
        "xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\">" +
        "      <ItemShape>" +
        "        <t:BaseShape>IdOnly</t:BaseShape>" +
        "      </ItemShape>" +
        "      <ParentFolderIds>" +
        "        <t:DistinguishedFolderId Id=\"inbox\"/>" +
        "      </ParentFolderIds>" +
        "    </FindItem>";
    private static final String findOneItemResponse =        
        "    <m:FindItemResponse xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\" " +
        "xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" " +
        "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope\">\n" +
        "<m:ResponseMessages>\n" +
        "  <m:FindItemResponseMessage ResponseClass=\"Success\">\n" +
        "    <m:ResponseCode>NoError</m:ResponseCode>\n" +
        "    <m:RootFolder TotalItemsInView=\"10\" IncludesLastItemInRange=\"true\">\n" +
        "      <t:Items>\n" +
        "        <t:Message>\n" +
        "          <t:ItemId Id=\"AS4AUn=\"/>\n" +
        "        </t:Message>\n" +
        "      </t:Items>\n" +
        "    </m:RootFolder>\n" +
        "   </m:FindItemResponseMessage>\n" +
        "</m:ResponseMessages>\n" +
        "</m:FindItemResponse>";
    
    private static String soap(String body)
    {
        return soapPrelude + body + soapFinale;
    }

    @Test
    public void testFindOneItem()
        throws UnsupportedEncodingException, XmlException, 
               HttpUrlConnectionCreationException, IOException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(url, soap(findItemsRequest).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, soap(findOneItemResponse).getBytes("UTF-8"));
        
        FindItemType findReq = FindItemDocument.Factory.parse(findItemsRequest).getFindItem();

        ExchangeService service = new ExchangeService(url, factory);
        FindItemResponseType response = service.findItem(findReq);
                
        FindItemResponseType expected = EnvelopeDocument.Factory.parse(soap(findOneItemResponse))
                                        .getEnvelope().getBody().getFindItemResponse();

        assertEquals(expected.xmlText(), response.xmlText());
    }
}
