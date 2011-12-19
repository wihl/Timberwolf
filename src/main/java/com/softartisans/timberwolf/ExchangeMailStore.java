package com.softartisans.timberwolf;


import com.microsoft.schemas.exchange.services._2006.messages.ExchangeService;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeServicePortType;
import com.microsoft.schemas.exchange.services.x2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemResponseDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.GetItemType;
import com.microsoft.schemas.exchange.services.x2006.messages.ItemInfoResponseMessageType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseItemIdsType;
import org.apache.cxf.Bus;
import org.apache.cxf.BusException;
import org.apache.cxf.BusFactory;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.cxf.wsdl.WSDLManager;
import org.apache.cxf.wsdl11.WSDLManagerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.ws.Holder;
import java.net.URL;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

/**
 * This is the MailStore implementation for Exchange email.
 * It uses Exchange's built in SOAP api to get emails from the server
 */
public class ExchangeMailStore implements MailStore
{
    private static final Logger log = LoggerFactory.getLogger(
            ExchangeMailStore.class);

    /**
     * GetItems takes multiple ids, but we don't want to call GetItems on all
     * MaxFindItemEntries at a time, because those could be massive responses
     * Instead, get a smaller number at a time.
     * This should evenly divide MaxFindItemEntries
     */
    private static final int MaxGetItemsEntries = 50;

    /**
     * The port of the service, passed in as a command line parameter, or from a
     * config
     */
    private ExchangeServicePortType exchangePort;

    public ExchangeMailStore(String exchangeUrl, String exchangeUser,
                             String exchangePassword)
    {
        Bus bus = BusFactory.getThreadDefaultBus();
        try
        {
            bus.setExtension(new WSDLManagerImpl(bus),WSDLManager.class);
        }
        catch (BusException e)
        {
            e.printStackTrace();
        }
        // create the object through which we interact with Exchange
        URL url = ExchangeService.class.getResource("/wsdl/Services.wsdl");
        ExchangeService service = new ExchangeService(url, ExchangeService.SERVICE);
        service.addPort(ExchangeService.ExchangePort, javax.xml.ws.soap.SOAPBinding.SOAP11HTTP_BINDING, exchangeUrl);
        exchangePort = service.getExchangePort();

        // add logging. I'm not sure if this integrates cleanly with log4j
        Client client = ClientProxy.getClient(exchangePort);
        client.getInInterceptors().add(new LoggingInInterceptor());
        client.getOutInterceptors().add(new LoggingOutInterceptor());

        // We must turn off chunking for authentication to work. I'm not sure why it would matter
        // see also 'A Note About Chunking':
        // https://cxf.apache.org/docs/client-http-transport-including-ssl-support.html
        HTTPConduit conduit = (HTTPConduit)client.getConduit();
        HTTPClientPolicy httpClientPolicy = new HTTPClientPolicy();
        httpClientPolicy.setAllowChunking(false);
        conduit.setClient(httpClientPolicy);

        // this only seems to matter if we're using NTLM
        // this is SPNEGO, which chooses a good authentication scheme at runtime
        // (either Kerberos or NTLM). It seems to always choose Kerberos
        // if we have a Kerberos ticket (check klist). If so it will ignore
        // this username and password
        AuthorizationPolicy auth = new AuthorizationPolicy();
        if (exchangeUser != null)
        {
            auth.setUserName(exchangeUser);
            auth.setPassword(exchangePassword);
        }
        conduit.setAuthorization(auth);

    }

    @Override
    public Iterable<MailboxItem> getMail()
    {
        return new Iterable<MailboxItem>()
        {
            @Override
            public Iterator<MailboxItem> iterator()
            {
                return new EmailIterator(exchangePort);
            }
        };
    }

    private static FindItemDocument getFindItemDocument()
    {
        FindItemDocument doc = FindItemDocument.Factory.newInstance();
        FindItemType type = doc.addNewFindItem();
        type.setTraversal(ItemQueryTraversalType.SHALLOW);
        ItemResponseShapeType shapeType = type.addNewItemShape();
        shapeType.setBaseShape(DefaultShapeNamesType.ID_ONLY);

        NonEmptyArrayOfBaseFolderIdsType folders = type.addNewParentFolderIds();
        DistinguishedFolderIdType distinguishedFolderIdType =
                folders.addNewDistinguishedFolderId();
        distinguishedFolderIdType.setId(DistinguishedFolderIdNameType.INBOX);
        return doc;
    }

    private static GetItemDocument getGetItemDocument(List<String> ids)
    {
        GetItemDocument doc = GetItemDocument.Factory.newInstance();
        GetItemType getItem = doc.addNewGetItem();
        getItem.addNewItemShape()
               .setBaseShape(DefaultShapeNamesType.ALL_PROPERTIES);
        NonEmptyArrayOfBaseItemIdsType items = getItem.addNewItemIds();
        for (String id : ids)
        {
            items.addNewItemId().setId(id);
        }
        return doc;
    }

    private static class EmailIterator implements Iterator<MailboxItem>
    {
        Vector<String> currentIds;
        int currentIdIndex = 0;
        int findItemsOffset = 0;
        ExchangeServicePortType exchangePort;
        private Vector<MailboxItem> mailBoxItems;
        private int currentMailboxItemIndex = 0;


        public EmailIterator(ExchangeServicePortType xchangePort)
        {
            this.exchangePort = xchangePort;
        }

        @Override
        public boolean hasNext()
        {
            if (currentIds == null)
            {
                currentMailboxItemIndex = 0;
                currentIdIndex = 0;
                currentIds = findItems(exchangePort);
                log.debug("Got " + currentIds.size() + " email ids");
            }
            // TODO paging here
            if (currentIdIndex >= currentIds.size())
            {
                return false;
            }
            if (mailBoxItems == null)
            {
                currentMailboxItemIndex = 0;
                mailBoxItems =
                        getItems(MaxGetItemsEntries, currentIdIndex, currentIds,
                                 exchangePort);
                log.debug("Got " + mailBoxItems.size() + " emails");
                return currentMailboxItemIndex < mailBoxItems.size();
            }
            // TODO call getItems more than once
            return currentMailboxItemIndex < mailBoxItems.size();
        }

        private static Vector<String> findItems(
                ExchangeServicePortType exchangePort)
        {
            Holder<FindItemResponseDocument> responses = new Holder<FindItemResponseDocument>();
            exchangePort.findItem(getFindItemDocument(),
                                  null, null, null, null, responses, null);
            ArrayOfResponseMessagesType array =
                    responses.value.getFindItemResponse()
                       .getResponseMessages();
            Vector<String> items = new Vector<String>();
            for (FindItemResponseMessageType message : array
                    .getFindItemResponseMessageArray())
            {
                log.debug(message.getResponseCode().toString());
                for (MessageType item : message.getRootFolder().getItems()
                                               .getMessageArray())
                {
                    items.add(item.getItemId().getId());
                }
            }
            return items;
        }

        private static Vector<MailboxItem> getItems(
                int count, int startIndex, Vector<String> ids,
                ExchangeServicePortType exchangePort)
        {
            int max = Math.min(startIndex + count, ids.size());
            if (max < startIndex)
            {
                return new Vector<MailboxItem>();
            }
            Holder<GetItemResponseDocument> response
                = new Holder<GetItemResponseDocument>();
            exchangePort.getItem(
                    getGetItemDocument(ids.subList(startIndex, max)),
                    null, null, null, null, response, null);
            ItemInfoResponseMessageType[] array =
                response.value.getGetItemResponse()
                .getResponseMessages()
                .getGetItemResponseMessageArray();
            Vector<MailboxItem> items = new Vector<MailboxItem>();
            for (ItemInfoResponseMessageType message : array)
            {
                    for (MessageType item : message.getItems()
                             .getMessageArray())
                    {
                        items.add(new ExchangeEmail(item));
                    }
            }
            return items;
        }

        @Override
        public MailboxItem next()
        {
            if (currentMailboxItemIndex < mailBoxItems.size())
            {
                MailboxItem item = mailBoxItems.get(currentMailboxItemIndex);
                currentMailboxItemIndex++;
                return item;
            }
            else
            {
                log.debug("All done, " + currentMailboxItemIndex + " >= "
                          + mailBoxItems.size());
                return null;
            }
        }

        @Override
        public void remove()
        {
        }
    }
}
