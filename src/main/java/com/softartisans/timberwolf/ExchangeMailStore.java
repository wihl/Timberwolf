package com.softartisans.timberwolf;


import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.frontend.ClientProxyFactoryBean;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.service.Service;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import com.microsoft.schemas.exchange.services._2006.messages.ExchangeService;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeServicePortType;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services.x2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services.x2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services.x2006.types.NonEmptyArrayOfBaseFolderIdsType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
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

        // create the object through which we interact with Exchange
        ExchangeService service = new ExchangeService();
        ExchangeServicePortType port = service.getExchangePort();

        // add logging. I'm not sure if this integrates cleanly with log4j
        Client client = ClientProxy.getClient(port);
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
    public Iterable<MailboxItem> getMail(String user)
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
                currentIds = findItems(findItemsOffset, exchangePort);
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
                int findItemsOffset, ExchangeServicePortType exchangePort)
        {

            return new Vector<String>();
        }

        private static Vector<MailboxItem> getItems(
                int maxGetItemsEntries, int currentIdIndex,
                Vector<String> currentIds, ExchangeServicePortType exchangePort)
        {
            return new Vector<MailboxItem>();
        }

        @Override
        public MailboxItem next()
        {
            return null;
        }

        @Override
        public void remove()
        {
        }
    }
}
