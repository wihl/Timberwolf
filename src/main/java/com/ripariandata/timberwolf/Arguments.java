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

import com.ripariandata.timberwolf.hbase.HBaseMailWriter;
import com.ripariandata.timberwolf.conf4j.ConfigEntry;

import org.kohsuke.args4j.Option;

/**
 * Arguments stores all the command line and configuration file arguments for
 * Timberwolf.
 */
public class Arguments
{
    private String configFileLocation = App.DEFAULT_CONFIG_LOCATION;
    private boolean isSetConfigFileLocation;
    private boolean help;
    private boolean isSetHelp;
    private String domain;
    private boolean isSetDomain;
    private String exchangeUrl;
    private boolean isSetExchangeUrl;
    private String hbaseQuorum;
    private boolean isSetHBaseQuorum;
    private String hbaseClientPort;
    private boolean isSetHBaseClientPort;
    private String hbaseTableName;
    private boolean isSetHBaseTableName;
    private String hbaseMetadataTableName;
    private boolean isSetHBaseMetadataTableName;
    private String hbaseKeyHeader = HBaseMailWriter.DEFAULT_KEY_HEADER;
    private boolean isSetHBaseKeyHeader;
    private String hbaseColumnFamily = HBaseMailWriter.DEFAULT_COLUMN_FAMILY;
    private boolean isSetHBaseColumnFamily;

    @Option(name = "--config",
            usage = "Sets the location to look for a configuration properties file.  Defaults to "
                    + App.DEFAULT_CONFIG_LOCATION + ".")
    private void setConfigFileLocation(String location)
    {
        configFileLocation = location;
        isSetConfigFileLocation = true;
    }

    public String configFileLocation()
    {
        return configFileLocation;
    }

    @Option(name = "-h", aliases = { "--help" },
            usage = "Show this help text.")
    private void setHelp(boolean h)
    {
        help = h;
        isSetHelp = true;
    }

    public boolean help()
    {
        return help;
    }

    @Option(name = "--domain",
            usage = "The domain you wish to crawl. Users of this domain will be imported.")
    @ConfigEntry(name = "domain")
    private void setDomain(String d)
    {
        domain = d;
        isSetDomain = true;
    }

    public String domain()
    {
        return domain;
    }

    @Option(name = "--exchange-url",
            usage = "The URL of your Exchange Web Services endpoint.\nFor example: "
                    + "https://example.com/ews/exchange.asmx")
    @ConfigEntry(name = "exchange.url")
    private void setExchangeUrl(String url)
    {
        exchangeUrl = url;
        isSetExchangeUrl = true;
    }

    public String exchangeUrl()
    {
        return exchangeUrl;
    }

    @Option(name = "--hbase-quorum",
            usage = "The ZooKeeper quorum used to connect to HBase.")
    @ConfigEntry(name = "hbase.quorum")
    private void setHBaseQuorum(String quorum)
    {
        hbaseQuorum = quorum;
        isSetHBaseQuorum = true;
    }

    public String hbaseQuorum()
    {
        return hbaseQuorum;
    }

    @Option(name = "--hbase-clientport",
            usage = "The ZooKeeper client port used to connect to HBase.")
    @ConfigEntry(name = "hbase.clientport")
    private void setHBaseClientPort(String port)
    {
        hbaseClientPort = port;
        isSetHBaseClientPort = true;
    }

    public String hbaseClientPort()
    {
        return hbaseClientPort;
    }

    @Option(name = "--hbase-table",
            usage = "The HBase table name that email data will be imported into.")
    @ConfigEntry(name = "hbase.table")
    private void setHBaseTableName(String table)
    {
        hbaseTableName = table;
        isSetHBaseTableName = true;
    }

    public String hbaseTableName()
    {
        return hbaseTableName;
    }

    @Option(name = "--hbase-metadata-table",
            usage = "The HBase table that will store timberwolf metatdata, such as the last time that we gathered "
                  + "email for each user.")
    @ConfigEntry(name = "hbase.metadatatable")
    private void setHBaseMetadataTableName(String table)
    {
        hbaseMetadataTableName = table;
        isSetHBaseMetadataTableName = true;
    }

    public String hbaseMetadataTableName()
    {
        return hbaseMetadataTableName;
    }

    @Option(name = "--hbase-key-header.",
            usage = "The header id to use as a row key for the imported email data.  Default row key is 'Item ID'.")
    @ConfigEntry(name = "hbase.key.header")
    private void setHBaseKeyHeader(String header)
    {
        hbaseKeyHeader = header;
        isSetHBaseKeyHeader = true;
    }

    public String hbaseKeyHeader()
    {
        return hbaseKeyHeader;
    }

    @Option(name = "--hbase-column-family.",
            usage = "The column family for the imported email data.  Default family is 'h'.")
    @ConfigEntry(name = "hbase.column.family")
    private void setHBaseColumnFamily(String family)
    {
        hbaseColumnFamily = family;
        isSetHBaseColumnFamily = true;
    }

    public String hbaseColumnFamily()
    {
        return hbaseColumnFamily;
    }

    /** Merges another Arguments object in to this one, taking the other args if possible. */
    public void mergeArguments(Arguments other)
    {
        if (other.isSetConfigFileLocation)
        {
            configFileLocation = other.configFileLocation;
        }

        if (other.isSetHelp)
        {
            help = other.help;
        }

        if (other.isSetDomain)
        {
            domain = other.domain;
        }

        if (other.isSetExchangeUrl)
        {
            exchangeUrl = other.exchangeUrl;
        }

        if (other.isSetHBaseQuorum)
        {
            hbaseQuorum = other.hbaseQuorum;
        }

        if (other.isSetHBaseClientPort)
        {
            hbaseClientPort = other.hbaseClientPort;
        }

        if (other.isSetHBaseTableName)
        {
            hbaseTableName = other.hbaseTableName;
        }

        if (other.isSetHBaseMetadataTableName)
        {
            hbaseMetadataTableName = other.hbaseMetadataTableName;
        }

        if (other.isSetHBaseKeyHeader)
        {
            hbaseKeyHeader = other.hbaseKeyHeader;
        }

        if (other.isSetHBaseColumnFamily)
        {
            hbaseColumnFamily = other.hbaseColumnFamily;
        }
    }
}
