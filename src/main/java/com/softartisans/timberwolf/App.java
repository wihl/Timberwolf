package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import com.cloudera.alfredo.client.AuthenticatedURL;

import org.apache.log4j.BasicConfigurator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Driver class to grab emails and put them in HBase.
 */
final class App
{
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

    @Option(required = false, name = "--hbase-quorum",
            usage = "The Zookeeper quorum used to connect to HBase.")
    private String hbaseQuorum;

    @Option(required = false, name = "--hbase-port",
            usage = "The port used to connect to HBase.")
    private String hbasePort;

    @Option(required = false, name = "--hbase-table",
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
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println("java timberwolf [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        Logger log = LoggerFactory.getLogger(App.class);
        BasicConfigurator.configure();
        log.info("Timberwolf invoked with the following arguments:");
        log.info("Exchange URL: {}", exchangeUrl);
        log.info("Exchange User: {}", exchangeUser);
        log.info("Exchange Password: {}", exchangePassword);
        log.info("Target User: {}", targetUser);
        log.info("HBase Quorum: {}", hbaseQuorum);
        log.info("HBase Port: {}", hbasePort);
        log.info("HBase Table Name: {}", hbaseTableName);
        log.info("HBase Column Family: {}", hbaseColumnFamily);

        ExchangeService service = new ExchangeService(exchangeUrl);
        for (MailboxItem item : service.getMail("bkerr"))
        {
            System.out.println(item.getHeader("Body"));
        }
    }
}
