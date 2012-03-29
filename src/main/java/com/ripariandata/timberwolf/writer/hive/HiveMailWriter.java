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

import com.ripariandata.timberwolf.MailboxItem;
import com.ripariandata.timberwolf.writer.MailWriter;

import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;
import java.util.UUID;

import org.apache.commons.lang.StringUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**Writes mail into a Hadoop Hive database. */
public class HiveMailWriter implements MailWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(HiveMailWriter.class);
    public static final String DEFAULT_KEY_HEADER = "Item ID";
    public static final String[] VALUE_HEADER_KEYS;
    private static final Path TEMP_FOLDER = new Path("/tmp/timberwolf");
    private static final String DRIVER_NAME = "org.apache.hadoop.hive.jdbc.HiveDriver";

    private FileSystem hdfs;
    private Connection hive;
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

    public HiveMailWriter(final String hdfsUri, final String hiveUri, final String table)
    {
        tableName = table;

        URI hdfsLocation;
        try
        {
            hdfsLocation = new URI(hdfsUri);
        }
        catch (URISyntaxException e)
        {
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(hdfs + " is not a valid URI.", e));
        }
        getHdfs(hdfsLocation);

        loadHiveDriver();
        getHive(hiveUri);
    }

    public HiveMailWriter(final FileSystem fs, final Connection conn, final String table)
    {
        tableName = table;
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

    private void getHive(final String hiveUri)
    {
        try
        {
            hive = DriverManager.getConnection(hiveUri);
        }
        catch (SQLException e)
        {
            String msg = "Error opening connection to hive at " + hiveUri.toString();
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private boolean tableExists()
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

    private void createTable()
    {
        try
        {
            // We aren't using a PreparedStatement here since the escaping only really works for arguments,
            // not for table and column names.
            Statement statement = hive.createStatement();
            String[] createQueryTokens = {
                "create table", tableName,
                "(", StringUtils.join(VALUE_HEADER_KEYS, " string, ") , "string )",
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

    private void loadTempFile(final Path tempFile)
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
            hive.close();
        }
        catch (SQLException e)
        {
            String msg = "Error closing connection to Hive.";
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private void getHdfs(final URI hdfsUri)
    {
        try
        {
            hdfs = FileSystem.get(hdfsUri, new Configuration());
        }
        catch (IOException e)
        {
            String msg = "Cannot access HDFS filesystem at " + hdfsUri.toString();
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException(msg, e));
        }
    }

    private Path writeTemporaryFile(final Iterable<MailboxItem> mail)
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

    /**
     * Delete the temporary file in hdfs.
     *
     * This does not throw an exception because this may be called during
     * a stack rewind. If that's the case, we don't want to override the
     * already thrown exception. We just log.
     */
    private void deleteTempFile(final Path tempFile)
    {
        try
        {
            hdfs.delete(tempFile, false);
        }
        catch (IOException e)
        {
            LOG.warn("Error cleaning up temporary file: " + e.getMessage());
            LOG.debug("", e);
        }
    }

    private void closeHdfs()
    {
        try
        {
            hdfs.close();
        }
        catch (IOException e)
        {
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException("Error closing HDFS connection.", e));
        }
    }

    public void write(final Iterable<MailboxItem> mail)
    {
        if (!tableExists())
        {
            createTable();
        }

        Path tempFile = writeTemporaryFile(mail);
        try
        {
            loadTempFile(tempFile);
        }
        finally
        {
            deleteTempFile(tempFile);
        }
    }

    public void close()
    {
        closeHdfs();
        closeHive();
    }
}
