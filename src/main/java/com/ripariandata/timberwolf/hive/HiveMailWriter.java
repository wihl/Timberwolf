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
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

import java.util.ArrayList;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

public class HiveMailWriter implements MailWriter
{
    // This is a non-whitespace control character, so it should, I
    // hope, not show up in any of our data.
    private static final char COLUMN_SEPARATOR = 0x1F;
    private static final String ENCODING = "UTF-8";
    private static final String KEY_HEADER = "Item ID";

    public HiveMailWriter()
    {
    }

    private ArrayList<String> valueHeaders(MailboxItem mail)
    {
        ArrayList<String> headers = new ArrayList<String>();
        for (String header : mail.possibleHeaderKeys())
        {
            if (header != KEY_HEADER)
            {
                headers.add(mail.getHeader(header));
            }
        }
        return headers;
    }

    private void write(Iterable<MailboxItem> mails, SequenceFile.Writer writer)
        throws UnsupportedEncodingException, IOException
    {
        for (MailboxItem mail : mails)
        {
            Text key = new Text(mail.getHeader(KEY_HEADER));
            Text value = new Text(StringUtils.join(valueHeaders(mail), COLUMN_SEPARATOR));
            writer.append(key, value);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public void write(Iterable<MailboxItem> mails)
    {
        // Obviously, this is for testing, not reals.
        try
        {
            SequenceFile.Writer writer = SequenceFile.createWriter(new Configuration(),
                                                                   new FSDataOutputStream(System.out), Text.class,
                                                                   Text.class, SequenceFile.CompressionType.NONE, null);
            write(mails, writer);
            writer.syncFs();
            writer.close();
        }
        catch (UnsupportedEncodingException e)
        {
            System.out.println("There was an Unsupported Encoding Exception!");
            System.out.println(e.getMessage());
        }
        catch (IOException e)
        {
            System.out.println("There was an IO Exception!");
            System.out.println(e.getMessage());
        }
    }
}
