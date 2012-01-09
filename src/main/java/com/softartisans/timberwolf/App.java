package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;

import com.softartisans.timberwolf.exchange.ExchangeMailStore;
import com.softartisans.timberwolf.exchange.ExchangeRuntimeException;
import com.softartisans.timberwolf.exchange.HttpErrorException;
import com.softartisans.timberwolf.exchange.ServiceCallException;
import com.softartisans.timberwolf.hbase.HBaseMailWriter;

import java.io.IOException;

import java.net.HttpURLConnection;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver class to grab emails and put them in HBase.
 */
final class App
{
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    @Option(required = true, name = "--exchange-url",
            usage = "The URL of your Exchange Web Services endpoint.\nFor example: "
                    + "https://example.contoso.com/ews/exchange.asmx")
    private String exchangeUrl;

    @Option(name = "--hbase-quorum",
            usage = "The ZooKeeper quorum used to connect to HBase.")
    private String hbaseQuorum;

    @Option(name = "--hbase-clientport",
            usage = "The ZooKeeper client port used to connect to HBase.")
    private String hbaseclientPort;

    @Option(name = "--hbase-table",
            usage = "The HBase table name that email data will be imported "
                  + "into.")
    private String hbaseTableName;

    @Option(name = "--hbase-key-header.",
            usage = "The header id to use as a row key for the imported email data.  Default row key is 'Item ID'.")
    private String hbaseKeyHeader = HBaseMailWriter.DEFAULT_KEY_HEADER;

    @Option(name = "--hbase-column-family.",
            usage = "The column family for the imported email data.  Default family is 'h'.")
    private String hbaseColumnFamily = HBaseMailWriter.DEFAULT_COLUMN_FAMILY;

    private App()
    {
    }

    public static void main(final String[] args) throws IOException, AuthenticationException
    {
        new App().run(args);
    }

    private void run(final String[] args) throws IOException, AuthenticationException
    {
        boolean useHBase;

        CmdLineParser parser = new CmdLineParser(this);
        try
        {
            parser.parseArgument(args);

            LOG.debug("Timberwolf invoked with the following arguments:");
            LOG.debug("Exchange URL: {}", exchangeUrl);
            LOG.debug("HBase ZooKeeper Quorum: {}", hbaseQuorum);
            LOG.debug("HBase ZooKeeper Client Port: {}", hbaseclientPort);
            LOG.debug("HBase Table Name: {}", hbaseTableName);
            LOG.debug("HBase Key Header: {}", hbaseKeyHeader);
            LOG.debug("HBase Column Family: {}", hbaseColumnFamily);

            boolean noHBaseArgs =
                    hbaseQuorum == null && hbaseclientPort == null
                    && hbaseTableName == null;
            boolean allHBaseArgs =
                    hbaseQuorum != null && hbaseclientPort != null
                    && hbaseTableName != null;

            if (!noHBaseArgs && !allHBaseArgs)
            {
                throw new CmdLineException(parser, "HBase ZooKeeper Quorum, HBase ZooKeeper Client Port, and HBase "
                                           + "Table Name must all be specified if at least one is specified");
            }

            useHBase = allHBaseArgs;
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println("java timberwolf [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        MailWriter mailWriter;
        if (useHBase)
        {
            mailWriter = HBaseMailWriter.create(hbaseQuorum, hbaseclientPort, hbaseTableName, hbaseKeyHeader,
                                                hbaseColumnFamily);
        }
        else
        {
            mailWriter = new ConsoleMailWriter();
        }

        ExchangeMailStore mailStore = new ExchangeMailStore(exchangeUrl);
        try
        {
            mailWriter.write(mailStore.getMail());
        }
        catch (ExchangeRuntimeException e)
        {
            Throwable inner = e.getCause();
            if (inner instanceof HttpErrorException)
            {
                HttpErrorException httpError = (HttpErrorException) inner;
                if (httpError.getErrorCode() == HttpURLConnection.HTTP_UNAUTHORIZED)
                {
                    System.out.println("There was an authentication error connecting to Exchange or HBase.  "
                                       + "See the log for more details.");
                }
                else
                {
                    System.out.println("There was an HTTP " + httpError.getErrorCode()
                                       + " error connection to either Exchange or HBase.  "
                                       + "See the log for more details.");
                }
            }
            else if (inner instanceof ServiceCallException)
            {
                ServiceCallException serviceError = (ServiceCallException) inner;
                if (serviceError.getReason() == ServiceCallException.Reason.AUTHENTICATION)
                {
                    System.out.println("There was an authentication error connecting to Exchange or HBase.  "
                                       + "See the log for more details.");
                }
                else if (serviceError.getReason() == ServiceCallException.Reason.SOAP)
                {
                    System.out.println("There was a SOAP error connecting to Exchange: "
                                       + serviceError.getSoapError().toString() + "  See the log for more details.");
                }
                else
                {
                    System.out.println("There was an unknown error connecting to Exchange or HBase.  "
                                       + "See the log for more details.");
                }
            }
            else
            {
                System.out.println("There was an unknown error connection to Exchange or HBase.  "
                                   + "See the log for more details.");
            }
        }
    }
}
