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
package com.ripariandata.timberwolf.writer.hive;

import com.ripariandata.timberwolf.writer.Manager;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.lang.StringUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Manages a connection with hive; handing out writers to tables. */
public class HiveManager implements Manager
{
    private static final Logger LOG = LoggerFactory.getLogger(HiveManager.class);
    private static final String DRIVER_NAME = "org.apache.hadoop.hive.jdbc.HiveDriver";

    private Connection hive;
    private FileSystem hdfs;

    public HiveManager(final String hdfsUri, final String hiveUri)
    {
        URI hdfsLocation;
        try
        {
            hdfsLocation = new URI(hdfsUri);
        }
        catch (URISyntaxException e)
        {
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(hdfsUri + " is not a valid URI.", e));
        }
        hdfs = createHdfs(hdfsLocation);

        loadHiveDriver();
        hive = getHive(hiveUri);
    }

    public HiveManager(final FileSystem fs, final Connection conn)
    {
        hdfs = fs;
        hive = conn;
    }

    private void loadHiveDriver()
    {
        try
        {
            Class.forName(DRIVER_NAME);
        }
        catch (ClassNotFoundException e)
        {
            String msg = "Cannot load Hive JDBC driver " + DRIVER_NAME;
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    public boolean tableExists(final String tableName)
    {
        try
        {
            PreparedStatement statement = hive.prepareStatement("show tables ?");
            statement.setString(1, tableName);
            ResultSet showTableResult = statement.executeQuery();
            return showTableResult.next();
        }
        catch (SQLException e)
        {
            String msg = "Error determining if table " + tableName + "exists.";
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private static Connection getHive(final String hiveUri)
    {
        try
        {
            return DriverManager.getConnection(hiveUri);
        }
        catch (SQLException e)
        {
            String msg = "Error opening connection to hive at " + hiveUri.toString();
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private static FileSystem createHdfs(final URI hdfsUri)
    {
        try
        {
            return FileSystem.get(hdfsUri, new Configuration());
        }
        catch (IOException e)
        {
            String msg = "Cannot access HDFS filesystem at " + hdfsUri.toString();
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    public void createTable(final String tableName, final String[] headerKeys)
    {
        try
        {
            // We aren't using a PreparedStatement here since the escaping only really works for arguments,
            // not for table and column names.
            Statement statement = hive.createStatement();
            String[] createQueryTokens = {
                "create table", tableName,
                "(", StringUtils.join(headerKeys, " string, ") , "string )",
                "row format delimited fields terminated by '\\037'",
                "stored as sequencefile"
            };
            String createTableQuery = StringUtils.join(createQueryTokens, " ");
            statement.executeQuery(createTableQuery);
        }
        catch (SQLException e)
        {
            String msg = "Error creating table " + tableName;
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    public FileSystem getHdfs()
    {
        return hdfs;
    }

    public void loadTempFile(final String tableName, final Path tempFile)
    {
        try
        {
            // We aren't using a statement variable for the table name since the escaping will mess it up.
            PreparedStatement statement = hive.prepareStatement("load data inpath ? into table " + tableName);
            statement.setString(1, tempFile.toString());
            statement.executeQuery();
        }
        catch (SQLException e)
        {
            String msg = "Error loading data into table.";
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private void closeHive()
    {
        try
        {
            if (hive != null)
            {
                hive.close();
            }
        }
        catch (SQLException e)
        {
            String msg = "Error closing connection to Hive.";
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private void closeHdfs()
    {
        try
        {
            if (hdfs != null)
            {
                hdfs.close();
            }
        }
        catch (IOException e)
        {
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException("Error closing HDFS connection.", e));
        }
    }

    public final void close()
    {
        closeHdfs();
        closeHive();
    }
}
