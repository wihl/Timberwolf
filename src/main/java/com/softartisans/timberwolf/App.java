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


        String fi = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n               xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\">\n    <soap:Body>\n        <FindItem xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\"\n                  xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\"\n                  Traversal=\"Shallow\">\n            <ItemShape>\n                <t:BaseShape>AllProperties</t:BaseShape>\n            </ItemShape>\n            <ParentFolderIds>\n                <t:DistinguishedFolderId Id=\"inbox\"/>\n            </ParentFolderIds>\n        </FindItem>\n    </soap:Body>\n</soap:Envelope>";
        byte[] bytes = fi.getBytes("UTF-8");
//        InputStream findItems = App.class.getResourceAsStream("/findItems.xml");
        int totalLength = bytes.length;
//        ByteArrayOutputStream tempStream = new ByteArrayOutputStream();
//        byte[] buffer = new byte[1024];
//        int length = 0;
//        while ((length = findItems.read(buffer)) >= 0)
//        {
//            totalLength+=length;
//            tempStream.write(buffer);
//        }

        AuthenticatedURL.Token token = new AuthenticatedURL.Token();
        URL url = new URL(exchangeUrl);
        HttpURLConnection conn = new AuthenticatedURL().openConnection(url,
                                                                       token);
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("Content-Type", "text/xml");
        conn.setRequestProperty("Content-Length", "" + totalLength);
        conn.getOutputStream().write(bytes);

        System.out.println();
        System.out.println("Token value: " + token);
        System.out.println("Status code: " + conn.getResponseCode() + " " + conn.getResponseMessage());
        System.out.println();
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
            }
            reader.close();
        }
        System.out.println();
    }
}
