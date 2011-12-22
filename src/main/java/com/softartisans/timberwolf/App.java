package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;
import com.softartisans.timberwolf.exchange.ExchangeMailStore;
import com.softartisans.timberwolf.exchange.ExchangeRuntimeException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Driver class to grab emails and put them in HBase.
 */
final class App
{
    private static final Logger LOG = LoggerFactory.getLogger(App.class);

    @Option(required = true, name = "--exchange-url",
            usage = "The URL of your Exchange Web Services endpoint.\nFor "
                  + "example: https://example.contoso.com/ews/exchange.asmx")
    private String exchangeUrl;

    @Option(required = false, name = "--exchange-user",
            usage = "The username that will be used to authenticate with "
                  + "Exchange Web Services.")
    private String exchangeUser;

    @Option(required = false, name = "--exchange-password",
            usage = "The password that will be used to authenticate with "
                  + "Exchange Web Services.")
    private String exchangePassword;

    @Option(required = false, name = "--get-email-for",
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
            throws IOException, AuthenticationException
    {
        new App().run(args);
    }

    private void run(final String[] args)
            throws IOException, AuthenticationException
    {
        CmdLineParser parser = new CmdLineParser(this);

        try
        {
            parser.parseArgument(args);

            LOG.debug("Timberwolf invoked with the following arguments:");
            LOG.debug("Exchange URL: {}", exchangeUrl);
            LOG.debug("Exchange User: {}", exchangeUser);
            LOG.debug("Exchange Password: {}", exchangePassword);
            LOG.debug("Target User: {}", targetUser);
            LOG.debug("HBase Quorum: {}", hbaseQuorum);
            LOG.debug("HBase Port: {}", hbasePort);
            LOG.debug("HBase Table Name: {}", hbaseTableName);
            LOG.debug("HBase Column Family: {}", hbaseColumnFamily);

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
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println("java timberwolf [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        ExchangeMailStore mailStore = new ExchangeMailStore(exchangeUrl);

        try
        {
            new ConsoleMailWriter().write(mailStore.getMail());
        }
        catch (ExchangeRuntimeException e)
        {
            System.err.println(
                "There was an error downloading messages from the Exchange server.  See log for details.");
        }
    }
}
