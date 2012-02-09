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

import java.util.Iterator;

public class HiveMailWriter implements MailWriter
{
    // Both of these are non-whitespace control characters, so they should, I
    // hope, not show up in any of our data.
    private static final char COLUMN_SEPARATOR = 0x1F;
    private static final char ROW_SEPARATOR = 0x1E;
    private static final String ENCODING = "UTF-8";

    public HiveMailWriter()
    {
    }

    private void write(String value, OutputStream output, boolean separate)
        throws UnsupportedEncodingException, IOException
    {
        if (separate)
        {
            output.write((int)COLUMN_SEPARATOR);
        }

        if (value != null)
        {
            output.write(value.getBytes(ENCODING));
        }
    }

    private void write(MailboxItem mail, OutputStream output, boolean separate)
        throws UnsupportedEncodingException, IOException
    {
        if (separate)
        {
            output.write((int)ROW_SEPARATOR);
        }

        String[] keys = mail.possibleHeaderKeys();
        if (keys.length > 0)
        {
            write(mail.getHeader(keys[0]), output, false);
        }

        for (int i = 1; i < keys.length; i++)
        {
            write(mail.getHeader(keys[i]), output, true);
        }
    }

    private void write(Iterator<MailboxItem> mails, OutputStream output)
        throws UnsupportedEncodingException, IOException
    {
        if (mails.hasNext())
        {
            MailboxItem mail = mails.next();
            write(mail, output, false);
        }

        while(mails.hasNext())
        {
            MailboxItem mail = mails.next();
            write(mail, output, true);
        }
    }

    @Override
    public void write(Iterable<MailboxItem> mails)
    {
        // Obviously, this is for testing, not reals.
        try
        {
            write(mails.iterator(), System.out);
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
