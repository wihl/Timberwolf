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
package com.ripariandata.timberwolf.hive;

import com.ripariandata.timberwolf.MailWriter;
import com.ripariandata.timberwolf.MailboxItem;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HiveMailWriter implements MailWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(HiveMailWriter.class);
    public static final String DEFAULT_KEY_HEADER = "Item ID";
    public static final String[] VALUE_HEADER_KEYS;
    private static final Path TEMP_FOLDER = new Path("/tmp/timberwolf");
    private static final String DRIVER_NAME = "org.apache.hadoop.hive.jdbc.HiveDriver";

    private URI hdfsUri;
    private URI hiveUri;
    private String tableName;

    static
    {
        String[] possible = MailboxItem.possibleHeaderKeys();
        ArrayList<String> values = new ArrayList<String>();
        for (String header : possible)
        {
            if (header != DEFAULT_KEY_HEADER)
            {
                values.add(header);
            }
        }
        VALUE_HEADER_KEYS = values.toArray(new String[possible.length - 1]);
    }

    public HiveMailWriter(URI hdfs, URI hive, String table)
    {
        tableName = table;
        hdfsUri = hdfs;
        hiveUri = hive;
    }

    public HiveMailWriter(String hdfs, String hive, String table)
    {
        tableName = table;
        try
        {
            hdfsUri = new URI(hdfs);
        }
        catch (URISyntaxException e)
        {
            HiveMailWriterException.log(LOG, new HiveMailWriterException(hdfs + " is not a valid URI.", e));
        }

        try
        {
            hiveUri = new URI(hive);
        }
        catch(URISyntaxException e)
        {
            HiveMailWriterException.log(LOG, new HiveMailWriterException(hive + " is not a valid URI.", e));
        }
    }

    private Connection getHive()
    {
        Connection hive;
        try
        {
            hive = DriverManager.getConnection(hiveUri.toString());
        }
        catch (SQLException e)
        {
            String msg = "Error opening connection to hive at " + hiveUri.toString();
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
        return hive;
    }

    private boolean tableExists(Connection conn)
    {
        try
        {
            PreparedStatement statement = conn.prepareStatement("show tables ?");
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

    private void createTable(Connection conn)
    {
        try
        {
            // We aren't using a PreparedStatement here since the escaping only really works for arguments,
            // not for table and column names.
            Statement statement = conn.createStatement();
            String[] createQueryTokens = { "create table", tableName,
                                           "(", StringUtils.join(VALUE_HEADER_KEYS, " string, ") , "string )",
                                           "row format delimited fields terminated by '\\037'",
                                           "stored as sequencefile" };
            String createTableQuery = StringUtils.join(createQueryTokens, " ");
            statement.executeQuery(createTableQuery);
        }
        catch (SQLException e)
        {
            String msg = "Error creating table " + tableName;
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private void loadTempFile(Connection conn, Path tempFile)
    {
        try
        {
            // We aren't using a statement variable for the table name since the escaping will mess it up.
            PreparedStatement statement = conn.prepareStatement("load data inpath ? into table " + tableName);
            statement.setString(1, tempFile.toString());
            statement.executeQuery();
        }
        catch (SQLException e)
        {
            String msg = "Error loading data into table.";
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private void cleanupHive(Connection conn)
    {
        try
        {
            conn.close();
        }
        catch (SQLException e)
        {
            String msg = "Error closing connection to Hive.";
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private FileSystem getHdfs()
    {
        FileSystem fs;
        try
        {
            fs = FileSystem.get(hdfsUri, new Configuration());
        }
        catch (IOException e)
        {
            String msg = "Cannot access HDFS filesystem at " + hdfsUri.toString();
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
        return fs;
    }

    private Path writeTemporaryFile(FileSystem hdfs, Iterable<MailboxItem> mail)
    {
        Path tempFile;
        try
        {
            if (!hdfs.exists(TEMP_FOLDER))
            {
                hdfs.mkdirs(TEMP_FOLDER);
            }

            tempFile = new Path(TEMP_FOLDER + "/" + UUID.randomUUID().toString());
            FSDataOutputStream output = hdfs.create(tempFile);
            SequenceFileMailWriter writer = new SequenceFileMailWriter(output);
            writer.write(mail);
            output.close();
        }
        catch (IOException e)
        {
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException("Error writing temporary file.", e));
        }
        return tempFile;
    }

    private void cleanupFileSystem(FileSystem hdfs, Path tempFile)
    {
        try
        {
            hdfs.delete(tempFile, false);
            hdfs.close();
        }
        catch (IOException e)
        {
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException("Error cleaning up temporary file.", e));
        }
    }

    public void write(Iterable<MailboxItem> mail)
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

        Connection hiveConn = getHive();
        if (!tableExists(hiveConn))
        {
            createTable(hiveConn);
        }

        FileSystem hdfs = getHdfs();
        Path tempFile = writeTemporaryFile(hdfs, mail);
        loadTempFile(hiveConn, tempFile);

        cleanupFileSystem(hdfs, tempFile);
        hiveConn.close();
    }
}
