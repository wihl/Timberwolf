package com.softartisans.timberwolf;

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
    @Option(required = true, name = "--exchange-url",
            usage = "The URL of your Exchange Web Services endpoint.\nFor "
                  + "example: https://example.contoso.com/ews/exchange.asmx")
    private String exchangeUrl;

    @Option(required = true, name = "--exchange-user",
            usage = "The username that will be used to authenticate with "
                  + "Exchange Web Services.")
    private String exchangeUser;

    @Option(required = true, name = "--exchange-password",
            usage = "The password that will be used to authenticate with "
                  + "Exchange Web Services.")
    private String exchangePassword;

    @Option(required = true, name = "--get-email-for",
            usage = "The user for whom to retrieve email.")
    private String targetUser;

    @Option(name = "--hbase-rootdir",
            usage = "The root directory used to connect to HBase.")
    private String hbaseRootDir;

    @Option(name = "--hbase-masterport",
            usage = "The port used to connect to HBase.")
    private String hbaseMasterPort;

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
            log.info("Exchange Password: {}", exchangePassword);
            log.info("Target User: {}", targetUser);
            log.info("HBase RootDir: {}", hbaseRootDir);
            log.info("HBase Master Port: {}", hbaseMasterPort);
            log.info("HBase Table Name: {}", hbaseTableName);
            log.info("HBase Key Header: {}", hbaseKeyHeader);
            log.info("HBase Column Family: {}", hbaseColumnFamily);

            boolean noHBaseArgs =
                    hbaseRootDir == null && hbaseMasterPort == null
                    && hbaseTableName == null;
            boolean allHBaseArgs =
                    hbaseRootDir != null && hbaseMasterPort != null
                    && hbaseTableName != null;

            // if no HBase args, write to console (for debugging).
            // Else, write to HBase

            if (!noHBaseArgs && !allHBaseArgs)
            {
                throw new CmdLineException(parser,
                        "HBase Root Dir, HBase Port, and HBase Table Name must"
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

    }
}
