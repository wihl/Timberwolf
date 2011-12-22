package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;
import com.softartisans.timberwolf.exchange.ExchangeMailStore;

import com.softartisans.timberwolf.hbase.HBaseMailWriter;
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
            usage = "The header id to use as a row key for the imported email "
                    + "data.  Default row key is 'Item ID'.")
    private String hbaseKeyHeader = "Item ID";

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
                throw new CmdLineException(parser,
                                           "HBase ZooKeeper Quorum, HBase ZooKeeper Client Port, "
                                           + "and HBase Table Name must all be specified if at"
                                           + "least one is specified");
            }

            // if no HBase args, write to console (for debugging).
            // Else, write to HBase
            MailWriter mailWriter;
            if (noHBaseArgs)
            {
                mailWriter = new ConsoleMailWriter();
            }
            else
            {
                mailWriter = HBaseMailWriter.create(hbaseQuorum,
                        hbaseclientPort, hbaseTableName, hbaseKeyHeader,
                        hbaseColumnFamily);
            }

            ExchangeMailStore mailStore = new ExchangeMailStore(exchangeUrl);
            mailWriter.write(mailStore.getMail());
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
}
