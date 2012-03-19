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

import java.net.URI;
import java.net.URISyntaxException;

import java.sql.SqlException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HiveMailWriter implements MailWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(HiveMailWriter.class);

    private URI hdfsUri;
    private URI hiveUri;
    private String tableName;

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

    public void write(Iterable<MailboxItem> mail)
    {
        Connection hiveConn = DriverManager.getConnection(hiveUri.toString());
        // TODO: How much sanitization do we need here?  Check for * and |?  Protect from all injection?
        String showTableQuery = "show tables '" + tableName + "'";
        LOG.trace("Verifying Hive table existence with query: " + showTableQuery);
        ResultSet showTableResult = conn.createStatement().executeQuery(showTableQuery);
        if (!showTableResult.next())
        {
            // TODO: The table doesn't exist, create it.
        }

        // TODO: Open HDFS connection (with `FileSystem.get`) to timberwolf's temporary folder.
        // TODO: Write sequence file into that temp folder.
        // TODO: Use Hive JDBC connection to use `load data` on the file we just wrote into the
        //       table from before.
        // TODO: Dispose of everything.
    }
}
