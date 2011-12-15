package com.softartisans.timberwolf;

import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Holder;

import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.configuration.security.AuthorizationPolicy;
import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.headers.Header;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.Interceptor;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.jaxws.handler.soap.SOAPHandlerInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.codehaus.plexus.util.cli.CommandLineException;
import org.junit.runners.ParentRunner;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.schemas.exchange.services._2006.messages.ArrayOfResponseMessagesType;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeService;
import com.microsoft.schemas.exchange.services._2006.messages.ExchangeServicePortType;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseType;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemType;
import com.microsoft.schemas.exchange.services._2006.messages.ResponseMessageType;
import com.microsoft.schemas.exchange.services._2006.types.BaseFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.DefaultShapeNamesType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdNameType;
import com.microsoft.schemas.exchange.services._2006.types.DistinguishedFolderIdType;
import com.microsoft.schemas.exchange.services._2006.types.ItemQueryTraversalType;
import com.microsoft.schemas.exchange.services._2006.types.ItemResponseShapeType;
import com.microsoft.schemas.exchange.services._2006.types.NonEmptyArrayOfBaseFolderIdsType;
import com.microsoft.schemas.exchange.services._2006.types.ServerVersionInfo;

/**
 * Driver class to grab emails and put them in HBase.
 */
final class App
{
    @Option(required = true, name = "--exchange-url",
            usage = "The URL of your Exchange Web Services endpoint.\nFor "
                  + "example: https://example.contoso.com/ews/exchange.asmx")
    private String exchangeUrl;

    @Option(name = "--exchange-user",
            usage = "The username that will be used to authenticate with "
                  + "Exchange Web Services.")
    private String exchangeUser;

    @Option(name = "--exchange-password",
            usage = "The password that will be used to authenticate with "
                  + "Exchange Web Services.")
    private String exchangePassword;

    @Option(required = true, name = "--get-email-for",
            usage = "The user for whom to retrieve email.")
    private String targetUser;

    @Option(name = "--hbase-quorum",
            usage = "The Zookeeper quorum used to connect to HBase.")
    private String hbaseQuorum;

    @Option(name = "--hbase-port",
            usage = "The port used to connect to HBase.")
    private String hbasePort;

    @Option(name = "--hbase-table",
            usage = "The HBase table name that email data will be imported "
                  + "into.")
    private String hbaseTableName;

    @Option(name = "--hbase-column-family.",
            usage = "The column family for the imported email data.  Default "
                  + "family is 'h'.")
    private String hbaseColumnFamily = "h";

    private App()
    {
    }

    public static void main(final String[] args)
    {
        new App().run(args);
    }

    private void run(final String[] args)
    {
        CmdLineParser parser = new CmdLineParser(this);

        try
        {
            parser.parseArgument(args);
            Logger log = LoggerFactory.getLogger(App.class);

            log.info("Timberwolf invoked with the following arguments:");
            log.info("Exchange URL: {}", exchangeUrl);
            log.info("Exchange User: {}", exchangeUser);
            log.info("Target User: {}", targetUser);
            log.info("HBase Quorum: {}", hbaseQuorum);
            log.info("HBase Port: {}", hbasePort);
            log.info("HBase Table Name: {}", hbaseTableName);
            log.info("HBase Column Family: {}", hbaseColumnFamily);

            if (exchangeUser != null && exchangePassword == null)
            {
                throw new CmdLineException(parser, "If you provide a username, you must also provide a password");
            }
            if (exchangeUser == null && exchangePassword != null)
            {
                throw new CmdLineException(parser, "If you provide a password, you must also provide a username");
            }

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
            
            boolean noHBaseArgs =
                    hbaseQuorum == null && hbasePort == null
                    && hbaseTableName == null;
            boolean allHBaseArgs =
                    hbaseQuorum != null && hbasePort != null
                    && hbaseTableName != null;

            // if no HBase args, write to console (for debugging).
            // Else, write to HBase

            if (!noHBaseArgs && !allHBaseArgs)
            {
                throw new CmdLineException(parser,
                        "HBase Quorum, HBase Port, and HBase Table Name must"
                        + " all be specified if at least one is specified");
            }
            if (noHBaseArgs)
            {
                ConsoleMailWriter mailWriter = new ConsoleMailWriter();
                ArrayList<MailboxItem> items = new ArrayList<MailboxItem>();
                //TODO: this code is pretty hackish, we should refactor soon  
                
                FindItemType request = createConsoleFindItemType();
                
                Holder<FindItemResponseType> responses = new Holder<FindItemResponseType>();
                port.findItem(request, null, null, null, null, responses, null);
                
                ArrayOfResponseMessagesType messages = responses.value.getResponseMessages();
                List<JAXBElement<? extends ResponseMessageType>> elements =
                        messages.getCreateItemResponseMessageOrDeleteItemResponseMessageOrGetItemResponseMessage();
                for (JAXBElement<? extends ResponseMessageType> element : elements)
                {
                    ResponseMessageType type = element.getValue();
                    if (type instanceof FindItemResponseMessageType)
                    {
                        FindItemResponseMessageType response = (FindItemResponseMessageType)type;
                        //esponse.
                        //TODO items.add(e);
                    }
                }
                
                mailWriter.write(items);
            }
            
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println("java timberwolf [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
            return;
        }
    }

    /**
     * Create a request to get some email items to display 
     * to the console so we know everything's working
     */
    private FindItemType createConsoleFindItemType()
    {
        FindItemType type = new FindItemType();
        type.setTraversal(ItemQueryTraversalType.SHALLOW);
        ItemResponseShapeType shapeType = new ItemResponseShapeType();
        shapeType.setBaseShape(DefaultShapeNamesType.ID_ONLY);
        type.setItemShape(shapeType);

        NonEmptyArrayOfBaseFolderIdsType folders = new NonEmptyArrayOfBaseFolderIdsType();
        DistinguishedFolderIdType distinguishedFolderIdType = new DistinguishedFolderIdType();
        distinguishedFolderIdType.setId(DistinguishedFolderIdNameType.INBOX);
        folders.getFolderIdOrDistinguishedFolderId().add(distinguishedFolderIdType);
        type.setParentFolderIds(folders);
        return type;
    }
}
