package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindFolderType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

import org.apache.xmlbeans.XmlException;
import org.junit.Test;
import org.xmlsoap.schemas.soap.envelope.EnvelopeDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.when;

/**
 * Test suite for the ExchangeService component.
 */
public class ExchangeServiceTest
{
    private static final String URL = "https://example.com/ews/exchange.asmx";
    private static final String SOAP_PRELUDE =
        "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                + "<soapenv:Body>";
    private static final String SOAP_FINALE =
        "</soapenv:Body>"
                + "</soapenv:Envelope>";
    private static final String FIND_ITEMS_REQUEST =
        "<FindItem Traversal=\"Shallow\" "
                + "xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\" "
                + "xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\">"
                + "      <ItemShape>"
                + "        <t:BaseShape>IdOnly</t:BaseShape>"
                + "      </ItemShape>"
                + "      <ParentFolderIds>"
                + "        <t:DistinguishedFolderId Id=\"inbox\"/>"
                + "      </ParentFolderIds>"
                + "    </FindItem>";
    private static final String FIND_ITEM_RESPONSE =
        "    <m:FindItemResponse xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\" "
                + "xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" "
                + "xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope\">\n"
                + "<m:ResponseMessages>\n"
                + "  <m:FindItemResponseMessage ResponseClass=\"Success\">\n"
                + "    <m:ResponseCode>NoError</m:ResponseCode>\n"
                + "    <m:RootFolder TotalItemsInView=\"10\" IncludesLastItemInRange=\"true\">\n"
                + "      <t:Items>\n"
                + "        <t:Message>\n"
                + "          <t:ItemId Id=\"AS4AUn=\"/>\n"
                + "        </t:Message>\n"
                + "      </t:Items>\n"
                + "    </m:RootFolder>\n"
                + "   </m:FindItemResponseMessage>\n"
                + "</m:ResponseMessages>\n"
                + "</m:FindItemResponse>";
    private static final String GET_ITEM_REQUEST =
        "<GetItem"
                + " xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\""
                + " xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\">"
                + "  <ItemShape>"
                + "    <t:BaseShape>Default</t:BaseShape>"
                + "    <t:IncludeMimeContent>true</t:IncludeMimeContent>"
                + "  </ItemShape>"
                + "  <ItemIds>"
                + "    <t:ItemId Id=\"AAAlAF\" ChangeKey=\"CQAAAB\"/>"
                + "  </ItemIds>"
                + "</GetItem>";
    private static final String GET_ITEM_RESPONSE =
        "<GetItemResponse xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\""
                + " xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\""
                + " xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\">"
                + "  <m:ResponseMessages>"
                + "    <m:GetItemResponseMessage ResponseClass=\"Success\">"
                + "      <m:ResponseCode>NoError</m:ResponseCode>"
                + "      <m:Items>"
                + "        <t:Message>"
                + "          <t:MimeContent CharacterSet=\"UTF-8\">UmVjZWl</t:MimeContent>"
                + "          <t:ItemId Id=\"AAAlAFVz\" ChangeKey=\"CQAAAB\" />"
                + "          <t:Subject />"
                + "          <t:Sensitivity>Normal</t:Sensitivity>"
                + "          <t:Body BodyType=\"HTML\">"
                + "           <![CDATA["
                + "            <html dir=\"ltr\">"
                + "              <head>"
                + "                <meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">"
                + "                  <meta content=\"MSHTML 6.00.3790.2759\" name=\"GENERATOR\">"
                + "                    <style title=\"owaParaStyle\">P { MARGIN-TOP: 0px; MARGIN-BOTTOM: 0px } </style>"
                + "                  </head>"
                + "              <body ocsi=\"x\">"
                + "                <div dir=\"ltr\">"
                + "                  <font face=\"Tahoma\" color=\"#000000\" size=\"2\"></font>&nbsp;"
                + "                </div>"
                + "              </body>"
                + "            </html>"
                + "           ]]>"
                + "          </t:Body>"
                + "          <t:Size>881</t:Size>"
                + "          <t:DateTimeSent>2006-10-28T01:37:06Z</t:DateTimeSent>"
                + "          <t:DateTimeCreated>2006-10-28T01:37:06Z</t:DateTimeCreated>"
                + "          <t:ResponseObjects>"
                + "            <t:ReplyToItem />"
                + "            <t:ReplyAllToItem />"
                + "            <t:ForwardItem />"
                + "          </t:ResponseObjects>"
                + "          <t:HasAttachments>false</t:HasAttachments>"
                + "          <t:ToRecipients>"
                + "            <t:Mailbox>"
                + "              <t:Name>User1</t:Name>"
                + "              <t:EmailAddress>User1@example.com</t:EmailAddress>"
                + "              <t:RoutingType>SMTP</t:RoutingType>"
                + "            </t:Mailbox>"
                + "          </t:ToRecipients>"
                + "          <t:IsReadReceiptRequested>false</t:IsReadReceiptRequested>"
                + "          <t:IsDeliveryReceiptRequested>false</t:IsDeliveryReceiptRequested>"
                + "          <t:From>"
                + "            <t:Mailbox>"
                + "              <t:Name>User2</t:Name>"
                + "              <t:EmailAddress>User2@example.com</t:EmailAddress>"
                + "              <t:RoutingType>SMTP</t:RoutingType>"
                + "            </t:Mailbox>"
                + "          </t:From>"
                + "          <t:IsRead>false</t:IsRead>"
                + "        </t:Message>"
                + "      </m:Items>"
                + "    </m:GetItemResponseMessage>"
                + "  </m:ResponseMessages>"
                + "</GetItemResponse>";

    private final String findFolderRequest = "<mes:FindFolder Traversal=\"Shallow\" "
    + "xmlns:mes=\"http://schemas.microsoft.com/exchange/services/2006/messages\" "
    + "xmlns:typ=\"http://schemas.microsoft.com/exchange/services/2006/types\">"
    + "<mes:FolderShape>"
    + "<typ:BaseShape>AllProperties</typ:BaseShape>"
    + "</mes:FolderShape>"
    + "<mes:ParentFolderIds>"
    + "<typ:DistinguishedFolderId Id=\"msgfolderroot\"/>"
    + "</mes:ParentFolderIds>"
    + "</mes:FindFolder>";

    private final String findFolderResponse = "<m:FindFolderResponse "
    + "xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\" "
    + "xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" "
    + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" "
    + "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\">"
    + "<m:ResponseMessages>"
    + "<m:FindFolderResponseMessage ResponseClass=\"Success\">"
    + "<m:ResponseCode>NoError</m:ResponseCode>"
    + "<m:RootFolder TotalItemsInView=\"12\" IncludesLastItemInRange=\"true\">"
    + "<t:Folders>"
    + "<t:Folder>"
    + "<t:FolderId Id=\"AAAWAGJrZXJyQGludC50YXJ0YXJ1cy5jb20ALgAAAAAAb"
    + "Ck8HJcmPEi9+6mY2w+80AEA+aDFFTolzk2yM0Sg+YQ84AAAAGxq3AAA\" "
    + "ChangeKey=\"AQAAABYAAAD5oMUVOiXOTbIzRKD5hDzgAAAAbbf5\"/>"
    + "<t:ParentFolderId Id=\"AAAWAGJrZXJyQGludC50YXJ0YXJ1cy5jb20ALgAAAAAAbCk8HJcmPEi9+6m"
    + "Y2w+80AEA+aDFFTolzk2yM0Sg+YQ84AAAAGxq1gAA\" ChangeKey=\"AQAAAA==\"/>"
    + "<t:FolderClass>IPF.Note</t:FolderClass>"
    + "<t:DisplayName>Deleted Items</t:DisplayName>"
    + "<t:TotalCount>0</t:TotalCount>"
    + "<t:ChildFolderCount>0</t:ChildFolderCount>"
    + "<t:UnreadCount>0</t:UnreadCount>"
    + "</t:Folder>"
    + "<t:Folder>"
    + "<t:FolderId Id=\"AAAWAGJrZXJyQGludC50YXJ0YXJ1cy5jb20ALgAAAAAAbCk8HJcmPEi9+6mY2w+80AEA+aDFFTo"
    + "lzk2yM0Sg+YQ84AAAAGxq5AAA\" ChangeKey=\"AQAAABYAAAD5oMUVOiXOTbIzRKD5hDzgAAAAbbf9\"/>"
    + "<t:ParentFolderId Id=\"AAAWAGJrZXJyQGludC50YXJ0YXJ1cy5jb20ALgAAAAAAbCk8HJ"
    + "cmPEi9+6mY2w+80AEA+aDFFTolzk2yM0Sg+YQ84AAAAGxq1gAA\" ChangeKey=\"AQAAAA==\"/>"
    + "<t:FolderClass>IPF.Note</t:FolderClass>"
    + "<t:DisplayName>Drafts</t:DisplayName>"
    + "<t:TotalCount>0</t:TotalCount>"
    + "<t:ChildFolderCount>0</t:ChildFolderCount>"
    + "<t:UnreadCount>0</t:UnreadCount>"
    + "</t:Folder>"
    + "<t:Folder>"
    + "<t:FolderId Id=\"AAAWAGJrZXJyQGludC50YXJ0YXJ1cy5jb20ALgAAAAAAbCk8HJcmPEi9+6mY2w+80AEA+aDFF"
    + "Tolzk2yM0Sg+YQ84AAAAGxq2QAA\" ChangeKey=\"AQAAABYAAAD5oMUVOiXOTbIzRKD5hDzgAAAAbhaq\"/>"
    + "<t:ParentFolderId Id=\"AAAWAGJrZXJyQGludC50YXJ0YXJ1cy5jb20ALgAAAAAAbCk8HJcmPEi9+6mY2w+80AEA+aDFFT"
    + "olzk2yM0Sg+YQ84AAAAGxq1gAA\" ChangeKey=\"AQAAAA==\"/>"
    + "<t:FolderClass>IPF.Note</t:FolderClass>"
    + "<t:DisplayName>Inbox</t:DisplayName>"
    + "<t:TotalCount>101</t:TotalCount>"
    + "<t:ChildFolderCount>0</t:ChildFolderCount>"
    + "<t:UnreadCount>100</t:UnreadCount>"
    + "</t:Folder>"
    + "</t:Folders>"
    + "</m:RootFolder>"
    + "</m:FindFolderResponseMessage>"
    + "</m:ResponseMessages>"
    + "</m:FindFolderResponse>";

    private static String soap(final String body)
    {
        return SOAP_PRELUDE + body + SOAP_FINALE;
    }

    @Test
    public void testFindItem()
        throws UnsupportedEncodingException, XmlException, ServiceCallException,
               IOException, HttpErrorException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(URL, soap(FIND_ITEMS_REQUEST).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, soap(FIND_ITEM_RESPONSE).getBytes("UTF-8"));

        FindItemType findReq = FindItemDocument.Factory.parse(FIND_ITEMS_REQUEST).getFindItem();

        ExchangeService service = new ExchangeService(URL, factory);
        FindItemResponseType response = service.findItem(findReq);

        FindItemResponseType expected = EnvelopeDocument.Factory.parse(soap(FIND_ITEM_RESPONSE))
                                        .getEnvelope().getBody().getFindItemResponse();

        assertEquals(expected.xmlText(), response.xmlText());
    }

    @Test
    public void testGetItem()
        throws UnsupportedEncodingException, XmlException, ServiceCallException,
               IOException, HttpErrorException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(URL, soap(GET_ITEM_REQUEST).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, soap(GET_ITEM_RESPONSE).getBytes("UTF-8"));

        GetItemType getReq = GetItemDocument.Factory.parse(GET_ITEM_REQUEST).getGetItem();

        ExchangeService service = new ExchangeService(URL, factory);
        GetItemResponseType response = service.getItem(getReq);

        GetItemResponseType expected = EnvelopeDocument.Factory.parse(soap(GET_ITEM_RESPONSE))
                                       .getEnvelope().getBody().getGetItemResponse();

        assertEquals(expected.toString(), response.toString());
    }

    @Test
    public void testFindFolder()
        throws UnsupportedEncodingException, XmlException, ServiceCallException, IOException, HttpErrorException
    {
       MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
       factory.forRequest(URL, soap(findFolderRequest).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, soap(findFolderResponse).getBytes("UTF-8"));

        FindFolderType findFolder = FindFolderDocument.Factory.parse(findFolderRequest).getFindFolder();

        ExchangeService service = new ExchangeService(URL, factory);
        FindFolderResponseType response = service.findFolder(findFolder);

        FindFolderResponseType expected = EnvelopeDocument.Factory.parse(soap(findFolderResponse))
                .getEnvelope().getBody().getFindFolderResponse();

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
        when(factory.newInstance(URL, soap(FIND_ITEMS_REQUEST).getBytes("UTF-8")))
            .thenReturn(conn);

        ExchangeService service = new ExchangeService(URL, factory);
        FindItemType findReq = FindItemDocument.Factory.parse(FIND_ITEMS_REQUEST).getFindItem();

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
    public void testInputStreamException()
        throws UnsupportedEncodingException, ServiceCallException, XmlException, ServiceCallException,
               HttpErrorException, IOException
    {
        HttpUrlConnectionFactory factory = mock(HttpUrlConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        stub(conn.getInputStream()).toThrow(new IOException("Cannot read code."));
        when(factory.newInstance(URL, soap(FIND_ITEMS_REQUEST).getBytes("UTF-8"))).thenReturn(conn);

        ExchangeService service = new ExchangeService(URL, factory);
        FindItemType findReq = FindItemDocument.Factory.parse(FIND_ITEMS_REQUEST).getFindItem();

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
    public void testEmptyResponse()
        throws IOException, UnsupportedEncodingException, ServiceCallException, XmlException, HttpErrorException
    {
        HttpUrlConnectionFactory factory = mock(HttpUrlConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {}));
        when(factory.newInstance(URL, soap(FIND_ITEMS_REQUEST).getBytes("UTF-8"))).thenReturn(conn);

        ExchangeService service = new ExchangeService(URL, factory);
        FindItemType findReq = FindItemDocument.Factory.parse(FIND_ITEMS_REQUEST).getFindItem();

        try
        {
            service.findItem(findReq);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("Response has empty body.", e.getMessage());
        }
    }

    @Test
    public void testAvailableException()
        throws IOException, UnsupportedEncodingException, ServiceCallException, XmlException, HttpErrorException
    {
        HttpUrlConnectionFactory factory = mock(HttpUrlConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        InputStream response = mock(InputStream.class);
        stub(response.available()).toThrow(new IOException());
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(conn.getInputStream()).thenReturn(response);
        when(factory.newInstance(URL, soap(FIND_ITEMS_REQUEST).getBytes("UTF-8"))).thenReturn(conn);

        ExchangeService service = new ExchangeService(URL, factory);
        FindItemType findReq = FindItemDocument.Factory.parse(FIND_ITEMS_REQUEST).getFindItem();

        try
        {
            service.findItem(findReq);
            fail("No exception was thrown.");
        }
        catch (ServiceCallException e)
        {
            assertEquals("Error reading available bytes.", e.getMessage());
        }
    }

    @Test
    public void testUnparsableResponse()
        throws UnsupportedEncodingException, XmlException, HttpErrorException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(URL, soap(GET_ITEM_REQUEST).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, soap("Not a real response").getBytes("UTF-8"));


        GetItemType getReq = GetItemDocument.Factory.parse(GET_ITEM_REQUEST).getGetItem();
        ExchangeService service = new ExchangeService(URL, factory);

        try
        {
            GetItemResponseType response = service.getItem(getReq);
        }
        catch (ServiceCallException e)
        {
            assertEquals("Error parsing SOAP response.", e.getMessage());
        }
    }

    @Test
    public void testNoEnvelopeResponse()
            throws UnsupportedEncodingException, XmlException, HttpErrorException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(URL, soap(GET_ITEM_REQUEST).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK,
                       "<?xml version=\"1.0\" encoding=\"utf-8\"?>".getBytes("UTF-8"));

        GetItemType getReq = GetItemDocument.Factory.parse(GET_ITEM_REQUEST).getGetItem();
        ExchangeService service = new ExchangeService(URL, factory);

        try
        {
            GetItemResponseType response = service.getItem(getReq);
        }
        catch (ServiceCallException e)
        {
            assertEquals("Error parsing SOAP response.", e.getMessage());
        }
    }

    @Test
    public void testNoBodyResponse()
            throws UnsupportedEncodingException, XmlException, HttpErrorException
    {
        MockHttpUrlConnectionFactory factory = new MockHttpUrlConnectionFactory();
        factory.forRequest(URL, soap(GET_ITEM_REQUEST).getBytes("UTF-8"))
               .respondWith(HttpURLConnection.HTTP_OK, (
                       "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                               + "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">"
                               + "</soapenv:Envelope>").getBytes("UTF-8"));

        GetItemType getReq = GetItemDocument.Factory.parse(GET_ITEM_REQUEST).getGetItem();
        ExchangeService service = new ExchangeService(URL, factory);

        try
        {
            GetItemResponseType response = service.getItem(getReq);
        }
        catch (ServiceCallException e)
        {
            assertEquals("SOAP response did not contain a body.", e.getMessage());
        }
    }

    @Test
    public void testHttpErrorResponse()
        throws UnsupportedEncodingException, XmlException, ServiceCallException, IOException
    {
        final int byteArrayInputValue = 64;
        HttpUrlConnectionFactory factory = mock(HttpUrlConnectionFactory.class);
        HttpURLConnection conn = mock(HttpURLConnection.class);
        when(conn.getResponseCode()).thenReturn(HttpURLConnection.HTTP_INTERNAL_ERROR);
        when(conn.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[] {byteArrayInputValue,
                byteArrayInputValue, byteArrayInputValue}));
        when(factory.newInstance(URL, soap(FIND_ITEMS_REQUEST).getBytes("UTF-8"))).thenReturn(conn);

        ExchangeService service = new ExchangeService(URL, factory);
        FindItemType findReq = FindItemDocument.Factory.parse(FIND_ITEMS_REQUEST).getFindItem();

        try
        {
            service.findItem(findReq);
            fail("No exception was thrown.");
        }
        catch (HttpErrorException e)
        {
            assertEquals("There was an HTTP 500 error while sending a request.", e.getMessage());
        }
    }
}
