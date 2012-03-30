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

import java.util.ArrayList;
import java.util.UUID;

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

    private HiveManager hive;
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

    public HiveMailWriter(final HiveManager hiveManager, final String table)
    {
        hive = hiveManager;
        tableName = table;
    }

    private Path writeTempFile(final Iterable<MailboxItem> mail)
    {
        try
        {
            FileSystem hdfs = hive.getHdfs();
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
        catch (IOException e)
        {
            throw HiveMailWriterException.log(LOG, new HiveMailWriterException("Error writing temporary file.", e));
        }
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
            hive.getHdfs().delete(tempFile, false);
        }
        catch (IOException e)
        {
            LOG.warn("Error cleaning up temporary file: " + e.getMessage());
            LOG.debug("", e);
        }
    }

    public void write(final Iterable<MailboxItem> mail)
    {
        if (!hive.tableExists(tableName))
        {
            hive.createTable(tableName, VALUE_HEADER_KEYS);
        }

        Path tempFile = writeTempFile(mail);
        try
        {
            hive.loadTempFile(tableName, tempFile);
        }
        finally
        {
            deleteTempFile(tempFile);
        }
    }
}
