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
    private static final String findItemResponse =        
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
    private static final String getItemRequest =
        "<GetItem" +
        " xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\"" +
        " xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\">" +
        "  <ItemShape>" +
        "    <t:BaseShape>Default</t:BaseShape>" +
        "    <t:IncludeMimeContent>true</t:IncludeMimeContent>" +
        "  </ItemShape>" +
        "  <ItemIds>" +
        "    <t:ItemId Id=\"AAAlAF\" ChangeKey=\"CQAAAB\"/>" +
        "  </ItemIds>" +
        "</GetItem>";
    private static final String getItemResponse =
        "<GetItemResponse xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\"" +
        " xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\"" +
        " xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\">" +
        "  <m:ResponseMessages>" +
        "    <m:GetItemResponseMessage ResponseClass=\"Success\">" +
        "      <m:ResponseCode>NoError</m:ResponseCode>" +
        "      <m:Items>" +
        "        <t:Message>" +
        "          <t:MimeContent CharacterSet=\"UTF-8\">UmVjZWl</t:MimeContent>" +
        "          <t:ItemId Id=\"AAAlAFVz\" ChangeKey=\"CQAAAB\" />" +
        "          <t:Subject />" +
        "          <t:Sensitivity>Normal</t:Sensitivity>" +
        "          <t:Body BodyType=\"HTML\">" +
        "           <![CDATA[" +
        "            <html dir=\"ltr\">" +
        "              <head>" +
        "                <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">" +
        "                  <meta content=\"MSHTML 6.00.3790.2759\" name=\"GENERATOR\">" +
        "                    <style title=\"owaParaStyle\">P { MARGIN-TOP: 0px; MARGIN-BOTTOM: 0px } </style>" +
        "                  </head>" +
        "              <body ocsi=\"x\">" +
        "                <div dir=\"ltr\">" +
        "                  <font face=\"Tahoma\" color=\"#000000\" size=\"2\"></font>&nbsp;" +
        "                </div>" +
        "              </body>" +
        "            </html>" +
        "           ]]>" +
        "          </t:Body>" +
        "          <t:Size>881</t:Size>" +
        "          <t:DateTimeSent>2006-10-28T01:37:06Z</t:DateTimeSent>" +
        "          <t:DateTimeCreated>2006-10-28T01:37:06Z</t:DateTimeCreated>" +
        "          <t:ResponseObjects>" +
        "            <t:ReplyToItem />" +
        "            <t:ReplyAllToItem />" +
        "            <t:ForwardItem />" +
        "          </t:ResponseObjects>" +
        "          <t:HasAttachments>false</t:HasAttachments>" +
        "          <t:ToRecipients>" +
        "            <t:Mailbox>" +
        "              <t:Name>User1</t:Name>" +
        "              <t:EmailAddress>User1@example.com</t:EmailAddress>" +
        "              <t:RoutingType>SMTP</t:RoutingType>" +
        "            </t:Mailbox>" +
        "          </t:ToRecipients>" +
        "          <t:IsReadReceiptRequested>false</t:IsReadReceiptRequested>" +
        "          <t:IsDeliveryReceiptRequested>false</t:IsDeliveryReceiptRequested>" +
        "          <t:From>" +
        "            <t:Mailbox>" +
        "              <t:Name>User2</t:Name>" +
        "              <t:EmailAddress>User2@example.com</t:EmailAddress>" +
        "              <t:RoutingType>SMTP</t:RoutingType>" +
        "            </t:Mailbox>" +
        "          </t:From>" +
        "          <t:IsRead>false</t:IsRead>" +
        "        </t:Message>" +
        "      </m:Items>" +
        "    </m:GetItemResponseMessage>" +
        "  </m:ResponseMessages>" +
        "</GetItemResponse>";
    
    private static String soap(String body)
    {
        return soapPrelude + body + soapFinale;
    }

    @Test
    public void testFindItem()
        throws UnsupportedEncodingException, XmlException, ServiceCallException, 
               IOException, HttpErrorException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(url, soap(findItemsRequest).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, soap(findItemResponse).getBytes("UTF-8"));
        
        FindItemType findReq = FindItemDocument.Factory.parse(findItemsRequest).getFindItem();

        ExchangeService service = new ExchangeService(url, factory);
        FindItemResponseType response = service.findItem(findReq);
                
        FindItemResponseType expected = EnvelopeDocument.Factory.parse(soap(findItemResponse))
                                        .getEnvelope().getBody().getFindItemResponse();

        assertEquals(expected.xmlText(), response.xmlText());
    }

    @Test
    public void testGetItem()
        throws UnsupportedEncodingException, XmlException, ServiceCallException, 
               IOException, HttpErrorException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(url, soap(getItemRequest).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, soap(getItemResponse).getBytes("UTF-8"));
        
        GetItemType getReq = GetItemDocument.Factory.parse(getItemRequest).getGetItem();

        ExchangeService service = new ExchangeService(url, factory);
        GetItemResponseType response = service.getItem(getReq);

        GetItemResponseType expected = EnvelopeDocument.Factory.parse(soap(getItemResponse))
                                       .getEnvelope().getBody().getGetItemResponse();

        assertEquals(expected.toString(), response.toString());
    }

    @Test
    public void testResponseCodeException()
        throws UnsupportedEncodingException, ServiceCallException, XmlException, ServiceCallException, 
               HttpErrorException, IOException
    {
        HttpUrlConnectionFactory factory = mock(HttpUrlConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        stub(conn.getResponseCode()).toThrow(new IOException("Cannot read code."));
        when(factory.newInstance(url, soap(findItemsRequest).getBytes("UTF-8")))
            .thenReturn(conn);

        ExchangeService service = new ExchangeService(url, factory);
        FindItemType findReq = FindItemDocument.Factory.parse(findItemsRequest).getFindItem();

        try
        {
            service.findItem(findReq);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {        
            assertEquals("Error getting HTTP status code.", e.getMessage());
            assertEquals(ServiceCallException.Reason.OTHER, e.getReason());
        }
    }

    @Test
    public void TestInputStreamException()
        throws UnsupportedEncodingException, ServiceCallException, XmlException, ServiceCallException, 
               HttpErrorException, IOException
    {
        HttpUrlConnectionFactory factory = mock(HttpUrlConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        stub(conn.getInputStream()).toThrow(new IOException("Cannot read code."));
        when(factory.newInstance(url, soap(findItemsRequest).getBytes("UTF-8")))
            .thenReturn(conn);

        ExchangeService service = new ExchangeService(url, factory);
        FindItemType findReq = FindItemDocument.Factory.parse(findItemsRequest).getFindItem();

        try
        {
            service.findItem(findReq);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {        
            assertEquals("Error getting input stream.", e.getMessage());
            assertEquals(ServiceCallException.Reason.OTHER, e.getReason());
        }            
    }

    @Test
    public void testUnparsableResponse()
        throws UnsupportedEncodingException, XmlException, HttpErrorException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(url, soap(getItemRequest).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, soap("Not a real response").getBytes("UTF-8"));
        
        GetItemType getReq = GetItemDocument.Factory.parse(getItemRequest).getGetItem();
        ExchangeService service = new ExchangeService(url, factory);

        try
        {
            GetItemResponseType response = service.getItem(getReq);
        }
        catch (ServiceCallException e)
        {
            assertEquals("Error parsing SOAP response.", e.getMessage());
        }
    }
}
