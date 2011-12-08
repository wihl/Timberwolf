package com.softartisans.timberwolf;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

/**
 * Driver class to grab emails and put them in HBase.
 */
final class App
{
    @Option(required=true, name="--exchange-url", 
            usage="The URL of your Exchange Web Services endpoint.\nFor " +
                  "example: https://example.contoso.com/ews/exchange.asmx")
    private String exchangeUrl;

    @Option(required=true, name="--exchange-user", 
            usage="The username that will be used to authenticate with " +
                  "Exchange Web Services.")
    private String exchangeUser;

    @Option(required=true, name="--exchange-password",
            usage="The password that will be used to authenticate with " +
                  "Exchange Web Services.")
    private String exchangePassword;

    @Option(required=true, name="--get-email-for",
            usage="The user for whom to retrieve emai.")
    private String targetUser;

    @Option(required=true, name="--hbase-quorum",
            usage="The Zookeeper quorum used to connect to HBase.")
    private String hbaseQuorum;

    @Option(required=true, name="--hbase-port",
            usage="The port used to connect to HBase.")
    private String hbasePort;

    @Option(required=true, name="--hbase-table",
            usage="The HBase table name that email data will be imported " +
                  "into.")
    private String hbaseTableName;

    @Option(name="--hbase-column-family.",
            usage="The column family for the imported email data.  Default " +
                  "family is 'h'.")
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
        }
        catch(CmdLineException e)
        {
            System.err.println(e.getMessage());
            System.err.println("java timberwolf [options...] arguments...");
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        System.out.print("exchange url: ");
        System.out.println(exchangeUrl);
        System.out.print("exchange user: ");
        System.out.println(exchangeUser);
        System.out.print("exchange password: ");
        System.out.println(exchangePassword);
        System.out.print("target user: ");
        System.out.println(targetUser);
        System.out.print("hbase quorum: ");
        System.out.println(hbaseQuorum);
        System.out.print("hbase port: ");
        System.out.println(hbasePort);
        System.out.print("hbase table name: ");
        System.out.println(hbaseTableName);
        System.out.print("hbase column family: ");
        System.out.println(hbaseColumnFamily);
    }
}
