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

import com.ripariandata.timberwolf.MailboxItem;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.ArrayList;

import org.apache.commons.lang.StringUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Excercises and verifies the functionality of SequenceFileMailWriter. */
public class SequenceFileMailWriterTest
{
    /** An entirely in-memory input stream that can be consumed by FSDataInputStream. */
    private class SeekablePositionedReadableByteArrayInputStream
        extends ByteArrayInputStream
        implements Seekable, PositionedReadable
    {
        public SeekablePositionedReadableByteArrayInputStream(final byte[] data)
        {
            super(data);
        }

        // PositionReadable methods

        public int read(final long position, final byte[] buffer, final int offset, final int length) throws IOException
        {
            this.mark(0);

            int r = 0;
            try
            {
                this.seek(position);
                r = this.read(buffer, offset, length);
            }
            finally
            {
                this.reset();
            }

            return r;
        }

        public void readFully(final long position, final byte[] buffer, final int offset, final int length)
            throws IOException
        {
            int r = this.read(position, buffer, offset, length);
            if (r != length)
            {
                throw new IOException();
            }
        }

        public void readFully(final long position, final byte[] buffer) throws IOException
        {
            this.readFully(position, buffer, 0, buffer.length);
        }

        // Seekable methods

        public void seek(final long pos) throws IOException
        {
            if (pos > this.count)
            {
                throw new IOException();
            }
            this.pos = (int) pos;
        }

        public long getPos() throws IOException
        {
            return this.pos;
        }

        public boolean seekToNewSource(final long targetPos) throws IOException
        {
            throw new IOException("I don't really know what this is supposed to do.");
        }
    }

    @SuppressWarnings("deprecation")
    private FileSystem mockFileSystem(final String path, final byte[] data) throws IOException
    {
        FileSystem fs = mock(FileSystem.class);
        Path fsPath = new Path(path);
        when(fs.open(eq(fsPath), any(int.class)))
            .thenReturn(new FSDataInputStream(new SeekablePositionedReadableByteArrayInputStream(data)));
        when(fs.getLength(eq(fsPath))).thenReturn((long) data.length);
        return fs;
    }

    @SuppressWarnings("deprecation")
    private SequenceFile.Reader writeMails(final Iterable<MailboxItem> mails) throws IOException
    {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        FSDataOutputStream output = new FSDataOutputStream(byteOut);
        SequenceFileMailWriter writer = new SequenceFileMailWriter(output);

        writer.write(mails);

        FileSystem fs = mockFileSystem("file", byteOut.toByteArray());
        return new SequenceFile.Reader(fs, new Path("file"), new Configuration());
    }

    @Test
    public void testWriteNothing() throws IOException
    {
        SequenceFile.Reader reader = writeMails(new ArrayList<MailboxItem>());

        Text key = new Text();
        Text value = new Text();
        assertFalse(reader.next(key, value));
    }

    private static MailboxItem mockMailboxItem(final String key, final String body, final String subject,
                                               final String timesent, final String sender, final String to,
                                               final String cc, final String bcc)
    {
        MailboxItem mail = mock(MailboxItem.class);
        ArrayList<String> headers = new ArrayList<String>();
        when(mail.hasKey(any(String.class))).thenReturn(true);
        when(mail.getHeader("Item ID")).thenReturn(key);
        if (body != null)
        {
            when(mail.getHeader("Body")).thenReturn(body);
            headers.add("Body");
        }
        if (subject != null)
        {
            when(mail.getHeader("Subject")).thenReturn(subject);
            headers.add("Subject");
        }
        if (timesent != null)
        {
            when(mail.getHeader("Time Sent")).thenReturn(timesent);
            headers.add("Time Sent");
        }
        if (sender != null)
        {
            when(mail.getHeader("Sender")).thenReturn(sender);
            headers.add("Sender");
        }
        if (to != null)
        {
            when(mail.getHeader("To")).thenReturn(to);
            headers.add("To");
        }
        if (cc != null)
        {
            when(mail.getHeader("Cc")).thenReturn(cc);
            headers.add("Cc");
        }
        if (bcc != null)
        {
            when(mail.getHeader("Bcc")).thenReturn(bcc);
            headers.add("Bcc");
        }
        when(mail.getHeaderKeys()).thenReturn(headers.toArray(new String[0]));
        return mail;
    }

    @Test
    public void testWriteAllHeaders() throws IOException
    {
        MailboxItem mail = mockMailboxItem("key", "Here's an email.", "Subject!!", "11 o'clock", "jim@example.com",
                                           "james@example.com", "j@example.com", "jane@example.com");
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mail);
        SequenceFile.Reader reader = writeMails(mails);

        char separator = 0x1F;
        Text key = new Text();
        Text value = new Text();
        assertTrue(reader.next(key, value));
        assertEquals("key", key.toString());
        assertEquals(StringUtils.join(new String[] { "Here's an email.", "Subject!!", "11 o'clock", "jim@example.com",
                                                     "james@example.com", "j@example.com", "jane@example.com" },
                                      separator), value.toString());
        assertFalse(reader.next(key, value));
    }

    @Test
    public void testWriteSomeHeaders() throws IOException
    {
        MailboxItem mail = mockMailboxItem("key", "Body of an email.", null, "12 o'clock", null, null, null,
                                           "j@example.com");
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mail);
        SequenceFile.Reader reader = writeMails(mails);

        char separator = 0x1F;
        Text key = new Text();
        Text value = new Text();
        assertTrue(reader.next(key, value));
        assertEquals("key", key.toString());
        assertEquals(StringUtils.join(new String[] { "Body of an email.", "", "12 o'clock", "", "", "",
                                                     "j@example.com" }, separator), value.toString());
        assertFalse(reader.next(key, value));
    }

    @Test
    public void testWriteManyMails() throws IOException
    {
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mockMailboxItem("key1", "BodyOne", "SubjectTwo", "TimeSentThree", null, null, null, null));
        mails.add(mockMailboxItem("key2", "BodyA", "SubjectB", "TimeSentC", null, null, null, null));
        mails.add(mockMailboxItem("key3", "BodyDee", "SubjectEee", "TimeSentEff", null, null, null, null));
        SequenceFile.Reader reader = writeMails(mails);

        char separator = 0x1F;
        Text key = new Text();
        Text value = new Text();
        assertTrue(reader.next(key, value));
        assertEquals("key1", key.toString());
        assertEquals(StringUtils.join(new String[] { "BodyOne", "SubjectTwo", "TimeSentThree", "", "", "", "" },
                                      separator), value.toString());
        assertTrue(reader.next(key, value));
        assertEquals("key2", key.toString());
        assertEquals(StringUtils.join(new String[] { "BodyA", "SubjectB", "TimeSentC", "", "", "", "" }, separator),
                     value.toString());
        assertTrue(reader.next(key, value));
        assertEquals("key3", key.toString());
        assertEquals(StringUtils.join(new String[] { "BodyDee", "SubjectEee", "TimeSentEff", "", "" , "", "" },
                                      separator), value.toString());
        assertFalse(reader.next(key, value));
    }

    @Test
    public void testWriteMailWithNoHeaders() throws IOException
    {
        MailboxItem mail = mock(MailboxItem.class);
        String[] headers = {"Item ID" };
        when(mail.getHeaderKeys()).thenReturn(headers);
        when(mail.hasKey(any(String.class))).thenReturn(true);
        when(mail.getHeader("Item ID")).thenReturn("key");
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mail);
        SequenceFile.Reader reader = writeMails(mails);

        char separator = 0x1F;
        Text key = new Text();
        Text value = new Text();
        assertTrue(reader.next(key, value));
        assertEquals("key", key.toString());
        assertEquals(StringUtils.join(new String[] { "", "", "", "", "", "", "" }, separator), value.toString());
    }

    /** An output stream that throws an exception whenever you try to write to it. */
    private class ExceptionalOutputStream extends OutputStream
    {
        @Override
        public void write(final int b) throws IOException
        {
            throw new IOException("Can't write to exceptional stream.");
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testWriteMailToStreamWithException() throws IOException
    {
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mockMailboxItem("key1", "body", "subject", "timesent", "sender", "to", "cc", "bcc"));
        OutputStream byteOut = new ExceptionalOutputStream();
        FSDataOutputStream output = new FSDataOutputStream(byteOut);
        SequenceFileMailWriter writer = new SequenceFileMailWriter(output);

        try
        {
            writer.write(mails);
            fail("Call to write should have thrown an exception.");
        }
        catch (HiveMailWriterException e)
        {
            // Our static analysis doesn't like empty blocks.
            assertTrue(true);
        }
        catch (Exception e)
        {
            fail("Wrong exception type.");
        }
    }
}
