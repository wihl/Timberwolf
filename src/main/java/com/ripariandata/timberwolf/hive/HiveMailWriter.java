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

    private boolean tableExists(Connection conn) throws SQLException
    {
        // TODO: How much sanitization do we need here?  Check for * and |?  Protect from all injection?
        String showTableQuery = "show tables '" + tableName + "'";
        LOG.trace("Verifying Hive table existence with query: " + showTableQuery);
        Statement statement = conn.createStatement();
        ResultSet showTableResult = statement.executeQuery(showTableQuery);
        return showTableResult.next();
    }

    private void createTable(Connection conn) throws SQLException
    {
        Statement statement = conn.createStatement();
        String[] createQueryTokens = { "create table", tableName,
                                        // TODO: Sanitize header key names.
                                       "(", StringUtils.join(VALUE_HEADER_KEYS, "string, ") , " string )",
                                       "row format delimited fields terminated by '\\037'",
                                       "stored as sequencefile" };
        String createTableQuery = StringUtils.join(createQueryTokens, " ");
        // TODO: Figure out what constitutes failure here.  No rows?  > 1 row?  Rows with particular contents?
        ResultSet createTableResult = statement.executeQuery(createTableQuery);
    }

    private Path writeTemporaryFile(FileSystem hdfs, Iterable<MailboxItem> mail) throws IOException
    {
        if (!hdfs.exists(TEMP_FOLDER))
        {
            hdfs.mkdirs(TEMP_FOLDER);
        }

        Path tempFile = new Path(TEMP_FOLDER + "/" + UUID.randomUUID().toString());
        FSDataOutputStream output = hdfs.create(tempFile);
        SequenceFileMailWriter writer = new SequenceFileMailWriter(output);
        writer.write(mail);
        output.close();

        return tempFile;
    }

    private void loadTempFile(Connection conn, Path tempFile) throws SQLException
    {
        Statement statement = conn.createStatement();
        String[] loadQueryTokens = { "load data inpath", "'" + tempFile.toString() + "'", "into table", tableName };
        String loadDataQuery = StringUtils.join(loadQueryTokens, " ");
        // TODO: Figure out what constitutes failure here.  No rows?  > 1 row?  Rows with particular contents?
        ResultSet loadDataResult = statement.executeQuery(loadDataQuery);
    }

    public void write(Iterable<MailboxItem> mail)
    {
        try
        {
            Connection hiveConn = DriverManager.getConnection(hiveUri.toString());
            if (!tableExists(hiveConn))
            {
                createTable(hiveConn);
            }

            Path tempFile;
            try
            {
                FileSystem hdfs = FileSystem.get(hdfsUri, new Configuration());
                tempFile = writeTemporaryFile(hdfs, mail);
                loadTempFile(hiveConn, tempFile);

                hdfs.delete(tempFile, false);
                hdfs.close();
            }
            catch (IOException e)
            {
                throw new HiveMailWriterException("", e);
                // TODO: Log properly.  In the function.
            }

            hiveConn.close();
        }
        catch (SQLException e)
        {
            // TODO: Log properly. In individual functions.
        }
    }
}
