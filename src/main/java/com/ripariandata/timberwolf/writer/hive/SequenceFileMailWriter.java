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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An implementation of MailWriter that creates sequence files suitable for use with Hive.
 */
public class SequenceFileMailWriter implements MailWriter
{
    private static final Logger LOG = LoggerFactory.getLogger(SequenceFileMailWriter.class);

    // This is a non-whitespace control character, so it should, I
    // hope, not show up in any of our data.
    private static final char COLUMN_SEPARATOR = 0x1F;
    private static final Map<String, String> ESCAPES = new HashMap<String, String>();

    private FSDataOutputStream outStream;

    static {
        // Hive doesn't handle newlines well at all, and they aren't particularly
        // useful from an analytics standpoint.
        ESCAPES.put("\n", " ");
    }

    public SequenceFileMailWriter(final FSDataOutputStream output)
    {
        outStream = output;
    }

    private static String escape(final String value)
    {
        if (value == null)
        {
            return value;
        }

        String ret = value;
        for (String s : ESCAPES.keySet())
        {
            ret = ret.replace(s, ESCAPES.get(s));
        }
        return ret;
    }

    private static ArrayList<String> valueHeaders(final MailboxItem mail)
    {
        ArrayList<String> headers = new ArrayList<String>();
        for (String header : HiveMailWriter.VALUE_HEADER_KEYS)
        {
            headers.add(escape(mail.getHeader(header)));
        }
        return headers;
    }

    private void write(final Iterable<MailboxItem> mails, final SequenceFile.Writer writer)
        throws IOException
    {
        for (MailboxItem mail : mails)
        {
            Text key = new Text(mail.getHeader(HiveMailWriter.DEFAULT_KEY_HEADER));
            Text value = new Text(StringUtils.join(valueHeaders(mail), COLUMN_SEPARATOR));
            writer.append(key, value);
        }
    }

    @Override
    public void write(final Iterable<MailboxItem> mails)
    {
        try
        {
            SequenceFile.Writer writer = SequenceFile.createWriter(new Configuration(), outStream, Text.class,
                                                                   Text.class, SequenceFile.CompressionType.NONE, null);
            write(mails, writer);
            writer.close();
        }
        catch (IOException e)
        {
            LOG.error("There was an error writing to the Hive file.");
            throw HiveMailWriterException.log(LOG,
                new HiveMailWriterException("There was an error writing to the Hive file", e));
        }
    }
}
