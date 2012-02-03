/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ripariandata.timberwolf;

import com.ripariandata.timberwolf.conf4j.ConfigEntry;
import com.ripariandata.timberwolf.conf4j.ConfigFileException;
import com.ripariandata.timberwolf.conf4j.ConfigFileMissingException;
import com.ripariandata.timberwolf.conf4j.ConfigFileParser;
import com.ripariandata.timberwolf.exchange.ExchangeMailStore;
import com.ripariandata.timberwolf.exchange.ExchangeRuntimeException;
import com.ripariandata.timberwolf.exchange.HttpErrorException;
import com.ripariandata.timberwolf.exchange.ServiceCallException;
import com.ripariandata.timberwolf.hbase.HBaseMailWriter;
import com.ripariandata.timberwolf.hbase.HBaseManager;
import com.ripariandata.timberwolf.hbase.HBaseUserTimeUpdater;
import com.ripariandata.timberwolf.services.LdapFetcher;
import com.ripariandata.timberwolf.services.PrincipalFetchException;
import com.ripariandata.timberwolf.services.PrincipalFetcher;

import java.io.IOException;
import java.io.PrintStream;

import java.net.HttpURLConnection;
import java.security.PrivilegedAction;

import javax.security.auth.login.LoginException;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Driver class to grab emails and put them in HBase.
 */
final class App implements PrivilegedAction<Integer>
{
    private static final String CONFIGURATION_ENTRY = "Timberwolf";
    private static final Logger LOG = LoggerFactory.getLogger(App.class);
    private static final String DEFAULT_CONFIG_LOCATION = "/etc/timberwolf.properties";

    /** This will get set to true if any hbase arguments are set. */
    private boolean useHBase;

    @Option(name = "--config",
            usage = "Sets the location to look for a configuration properties file.  Defaults to "
                    + DEFAULT_CONFIG_LOCATION + ".")
    private String configFileLocation = DEFAULT_CONFIG_LOCATION;

    // @Option(name = "-h", aliases = { "--help" },
    //         usage = "Show this help text.")
    private boolean help;

    // @Option(required = true, name = "--domain",
    //         usage = "The domain you wish to crawl. Users of this domain will be imported.")
    @ConfigEntry(name = "domain")
    private String domain;

    // @Option(required = true, name = "--exchange-url",
    //         usage = "The URL of your Exchange Web Services endpoint.\nFor example: "
    //                 + "https://example.com/ews/exchange.asmx")
    @ConfigEntry(name = "exchange.url")
    private String exchangeUrl;

    // @Option(name = "--hbase-quorum",
    //         usage = "The ZooKeeper quorum used to connect to HBase.")
    @ConfigEntry(name = "hbase.quorum")
    private String hbaseQuorum;

    // @Option(name = "--hbase-clientport",
    //         usage = "The ZooKeeper client port used to connect to HBase.")
    @ConfigEntry(name = "hbase.clientport")
    private String hbaseclientPort;

    // @Option(name = "--hbase-table",
    //         usage = "The HBase table name that email data will be imported into.")
    @ConfigEntry(name = "hbase.table")
    private String hbaseTableName;

    // @Option(name = "--hbase-metadata-table",
    //         usage = "The HBase table that will store timberwolf metatdata, such as the last time that we gathered "
    //               + "email for each user.")
    @ConfigEntry(name = "hbase.metadatatable")
    private String hbaseMetadataTableName;

    // @Option(name = "--hbase-key-header.",
    //         usage = "The header id to use as a row key for the imported email data.  Default row key is 'Item ID'.")
    @ConfigEntry(name = "hbase.key.header")
    private String hbaseKeyHeader = HBaseMailWriter.DEFAULT_KEY_HEADER;

    // @Option(name = "--hbase-column-family.",
    //         usage = "The column family for the imported email data.  Default family is 'h'.")
    @ConfigEntry(name = "hbase.column.family")
    private String hbaseColumnFamily = HBaseMailWriter.DEFAULT_COLUMN_FAMILY;

    private App()
    {
    }

    private static void printUsage(final PrintStream output, final CmdLineParser parser)
    {
        output.println("java -jar timberwolf.jar [options...] arguments...");
        parser.printUsage(output);
        output.println();
    }

    public static void main(final String[] args) throws IOException
    {
        new App().beginEverything(args);
    }

    private void beginEverything(final String[] args) throws IOException
    {
        ConfigFileParser configParser = new ConfigFileParser(this);
        CmdLineParser cliParser = new CmdLineParser(this);

        try
        {
            cliParser.parseArgument(args);
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            printUsage(System.err, cliParser);
        }

        try
        {
            configParser.parseConfigFile(configFileLocation);
        }
        catch (ConfigFileMissingException e)
        {
            if (configFileLocation != DEFAULT_CONFIG_LOCATION)
            {
                // Assume that this was specified explicitly.
                System.err.println(e.getMessage());
                return;
            }

            // If the config file was not explicitly named, assume that the user
            // meant to specify everything on the command line.
        }
        catch (ConfigFileException e)
        {
            System.err.println(e.getMessage());
            // TODO: print usage info for the config file?
            return;
        }

        try
        {
            if (help)
            {
                printUsage(System.out, cliParser);
                return;
            }

            LOG.debug("Timberwolf invoked with the following arguments:");
            LOG.debug("Domain: {}", domain);
            LOG.debug("Exchange URL: {}", exchangeUrl);
            LOG.debug("HBase ZooKeeper Quorum: {}", hbaseQuorum);
            LOG.debug("HBase ZooKeeper Client Port: {}", hbaseclientPort);
            LOG.debug("HBase Table Name: {}", hbaseTableName);
            LOG.debug("HBase Key Header: {}", hbaseKeyHeader);
            LOG.debug("HBase Column Family: {}", hbaseColumnFamily);

            boolean noHBaseArgs =
                    hbaseQuorum == null && hbaseclientPort == null
                    && hbaseTableName == null && hbaseMetadataTableName == null;
            boolean allHBaseArgs =
                    hbaseQuorum != null && hbaseclientPort != null
                    && hbaseTableName != null && hbaseMetadataTableName != null;

            if (!noHBaseArgs && !allHBaseArgs)
            {
                throw new CmdLineException(cliParser, "HBase ZooKeeper Quorum, HBase ZooKeeper Client Port, and HBase "
                                           + "Table Name must all be specified if at least one is specified");
            }

            useHBase = allHBaseArgs;

            Auth.authenticateAndDo(this, CONFIGURATION_ENTRY);
        }

        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            printUsage(System.err, cliParser);
        }
        catch (LoginException e)
        {
            System.err.println("Authentication failed: " + e.getMessage());
        }
    }

    public Integer run()
    {
        MailWriter mailWriter;
        UserTimeUpdater timeUpdater;
        HBaseManager hbaseManager = null;
        if (useHBase)
        {
            hbaseManager = new HBaseManager(hbaseQuorum, hbaseclientPort);
            mailWriter = HBaseMailWriter.create(hbaseManager, hbaseTableName, hbaseKeyHeader,
                                                hbaseColumnFamily);
            timeUpdater = new HBaseUserTimeUpdater(hbaseManager, hbaseMetadataTableName);
        }
        else
        {
            mailWriter = new ConsoleMailWriter();
            timeUpdater = new NoopUserTimeUpdater();
        }

        ExchangeMailStore mailStore = new ExchangeMailStore(exchangeUrl);
        try
        {
            PrincipalFetcher userLister = new LdapFetcher(domain);
            Iterable<String> users = userLister.getPrincipals();

            mailWriter.write(mailStore.getMail(users, timeUpdater));
            return 0;
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
        catch (PrincipalFetchException e)
        {
            System.out.println("There was a problem fetching a list of users from Active Directory. "
                               + e.getMessage() +  "See the log for more details.");
        }
        finally
        {
            if (hbaseManager != null)
            {
                hbaseManager.close();
            }
        }
        return 1;
    }
}
