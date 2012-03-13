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

/** Excercises and verifies the functionality of HiveMailWriter. */
public class HiveMailWriterTest
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
                if (r != length)
                {
                    throw new IOException();
                }
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
        HiveMailWriter writer = new HiveMailWriter(output);

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

    private static MailboxItem mockMailboxItem(final String key, final String alpha, final String beta,
                                               final String gamma)
    {
        MailboxItem mail = mock(MailboxItem.class);
        String[] headers = {"Item ID", "Alpha", "Beta", "Gamma" };
        when(mail.getHeaderKeys()).thenReturn(headers);
        when(mail.possibleHeaderKeys()).thenReturn(headers);
        when(mail.hasKey(any(String.class))).thenReturn(true);
        when(mail.getHeader("Item ID")).thenReturn(key);
        if (alpha != null)
        {
            when(mail.getHeader("Alpha")).thenReturn(alpha);
        }
        if (beta != null)
        {
            when(mail.getHeader("Beta")).thenReturn(beta);
        }
        if (gamma != null)
        {
            when(mail.getHeader("Gamma")).thenReturn(gamma);
        }
        return mail;
    }

    @Test
    public void testWriteAllHeaders() throws IOException
    {
        MailboxItem mail = mockMailboxItem("key", "One", "Two", "Three");
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mail);
        SequenceFile.Reader reader = writeMails(mails);

        char separator = 0x1F;
        Text key = new Text();
        Text value = new Text();
        assertTrue(reader.next(key, value));
        assertEquals("key", key.toString());
        assertEquals("One" + separator + "Two" + separator + "Three", value.toString());
        assertFalse(reader.next(key, value));
    }

    @Test
    public void testWriteSomeHeaders() throws IOException
    {
        MailboxItem mail = mockMailboxItem("key", "One", null, "Three");
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mail);
        SequenceFile.Reader reader = writeMails(mails);

        char separator = 0x1F;
        Text key = new Text();
        Text value = new Text();
        assertTrue(reader.next(key, value));
        assertEquals("key", key.toString());
        assertEquals("One" + separator + separator + "Three", value.toString());
        assertFalse(reader.next(key, value));
    }

    @Test
    public void testWriteManyMails() throws IOException
    {
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mockMailboxItem("key1", "One", "Two", "Three"));
        mails.add(mockMailboxItem("key2", "A", "B", "C"));
        mails.add(mockMailboxItem("key3", "Dee", "Eee", "Eff"));
        SequenceFile.Reader reader = writeMails(mails);

        char separator = 0x1F;
        Text key = new Text();
        Text value = new Text();
        assertTrue(reader.next(key, value));
        assertEquals("key1", key.toString());
        assertEquals("One" + separator + "Two" + separator + "Three", value.toString());
        assertTrue(reader.next(key, value));
        assertEquals("key2", key.toString());
        assertEquals("A" + separator + "B" + separator + "C", value.toString());
        assertTrue(reader.next(key, value));
        assertEquals("key3", key.toString());
        assertEquals("Dee" + separator + "Eee" + separator + "Eff", value.toString());
        assertFalse(reader.next(key, value));
    }

    @Test
    public void testWriteMailWithNoHeaders() throws IOException
    {
        MailboxItem mail = mock(MailboxItem.class);
        String[] headers = {"Item ID" };
        when(mail.getHeaderKeys()).thenReturn(headers);
        when(mail.possibleHeaderKeys()).thenReturn(headers);
        when(mail.hasKey(any(String.class))).thenReturn(true);
        when(mail.getHeader("Item ID")).thenReturn("key");
        ArrayList<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mail);
        SequenceFile.Reader reader = writeMails(mails);

        Text key = new Text();
        Text value = new Text();
        assertTrue(reader.next(key, value));
        assertEquals("key", key.toString());
        assertEquals("", value.toString());
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
        mails.add(mockMailboxItem("key1", "One", "Two", "Three"));
        OutputStream byteOut = new ExceptionalOutputStream();
        FSDataOutputStream output = new FSDataOutputStream(byteOut);
        HiveMailWriter writer = new HiveMailWriter(output);

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
