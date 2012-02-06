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
    static final String DEFAULT_CONFIG_LOCATION = "/etc/timberwolf.properties";

    /** This will get set to true if any hbase arguments are set. */
    private boolean useHBase;

    private Arguments arguments;

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
        Arguments cliargs = new Arguments();
        arguments = new Arguments();
        ConfigFileParser configParser = new ConfigFileParser(arguments);
        CmdLineParser cliParser = new CmdLineParser(cliargs);

        try
        {
            cliParser.parseArgument(args);
        }
        catch (CmdLineException e)
        {
            System.err.println(e.getMessage());
            printUsage(System.err, cliParser);
            return;
        }

        if (cliargs.help())
        {
            printUsage(System.out, cliParser);
            return;
        }

        try
        {
            configParser.parseConfigFile(cliargs.configFileLocation());
        }
        catch (ConfigFileMissingException e)
        {
            if (cliargs.configFileLocation() != DEFAULT_CONFIG_LOCATION)
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

        arguments.mergeArguments(cliargs);

        LOG.debug("Timberwolf invoked with the following arguments:");
        LOG.debug("Domain: {}", arguments.domain());
        LOG.debug("Exchange URL: {}", arguments.exchangeUrl());
        LOG.debug("HBase ZooKeeper Quorum: {}", arguments.hbaseQuorum());
        LOG.debug("HBase ZooKeeper Client Port: {}", arguments.hbaseClientPort());
        LOG.debug("HBase Table Name: {}", arguments.hbaseTableName());
        LOG.debug("HBase Metadata Table Name: {}", arguments.hbaseMetadataTableName());
        LOG.debug("HBase Key Header: {}", arguments.hbaseKeyHeader());
        LOG.debug("HBase Column Family: {}", arguments.hbaseColumnFamily());

        if (arguments.domain() == null)
        {
            System.err.println("The domain must be specified either in the configuration file or on the command line.");
            printUsage(System.err, cliParser);
            return;
        }

        if (arguments.exchangeUrl() == null)
        {
            System.err.println(
                "The Exchange URL must be specified either in the configuration file or on the command line.");
            printUsage(System.err, cliParser);
            return;
        }

        boolean noHBaseArgs = arguments.hbaseQuorum() == null && arguments.hbaseClientPort() == null
                           && arguments.hbaseTableName() == null && arguments.hbaseMetadataTableName() == null;
        boolean allHBaseArgs = arguments.hbaseQuorum() != null && arguments.hbaseClientPort() != null
                            && arguments.hbaseTableName() != null && arguments.hbaseMetadataTableName() != null;

        if (!noHBaseArgs && !allHBaseArgs)
        {
            System.err.println("HBase ZooKeeper Quorum, HBase ZooKeeper Client Port, and HBase Table Name must all be "
                             + "specified if at least one is specified");
            printUsage(System.err, cliParser);
            return;
        }
        useHBase = allHBaseArgs;

        try
        {
            Auth.authenticateAndDo(this, CONFIGURATION_ENTRY);
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
            hbaseManager = new HBaseManager(arguments.hbaseQuorum(), arguments.hbaseClientPort());
            mailWriter = HBaseMailWriter.create(hbaseManager, arguments.hbaseTableName(), arguments.hbaseKeyHeader(),
                                                arguments.hbaseColumnFamily());
            timeUpdater = new HBaseUserTimeUpdater(hbaseManager, arguments.hbaseMetadataTableName());
        }
        else
        {
            mailWriter = new ConsoleMailWriter();
            timeUpdater = new NoopUserTimeUpdater();
        }

        ExchangeMailStore mailStore = new ExchangeMailStore(arguments.exchangeUrl());
        try
        {
            PrincipalFetcher userLister = new LdapFetcher(arguments.domain());
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
