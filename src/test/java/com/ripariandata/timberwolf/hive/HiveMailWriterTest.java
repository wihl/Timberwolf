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

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.util.ArrayList;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.Seekable;
import org.apache.hadoop.fs.PositionedReadable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.SequenceFile;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;

public class HiveMailWriterTest
{
    private class SeekablePositionedReadableByteArrayInputStream
        extends ByteArrayInputStream
        implements Seekable, PositionedReadable
    {
        public SeekablePositionedReadableByteArrayInputStream(byte[] data)
        {
            super(data);
        }

        // PositionReadable methods

        public int read(long position, byte[] buffer, int offset, int length) throws IOException
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

        public void readFully(long position, byte[] buffer, int offset, int length) throws IOException
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
                if (r != length) { throw new IOException(); }
            }
        }

        public void readFully(long position, byte[] buffer) throws IOException
        {
            this.readFully(position, buffer, 0, buffer.length);
        }

        // Seekable methods

        public void seek(long pos) throws IOException
        {
            if (pos > this.count) { throw new IOException(); }
            this.pos = (int)pos;
        }

        public long getPos() throws IOException
        {
            return this.pos;
        }

        public boolean seekToNewSource(long targetPos) throws IOException
        {
            throw new IOException("I don't really know what this is supposed to do.");
        }
    }

    private FileSystem mockFileSystem(String path, byte[] data) throws IOException
    {
        FileSystem fs = mock(FileSystem.class);
        when(fs.open(eq(new Path(path)), any(int.class)))
            .thenReturn(new FSDataInputStream(new SeekablePositionedReadableByteArrayInputStream(data)));
        return fs;
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testWriteNothing() throws IOException
    {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
        FSDataOutputStream output = new FSDataOutputStream(byteOut);
        HiveMailWriter writer = new HiveMailWriter(output);

        writer.write(new ArrayList<MailboxItem>());

        FileSystem fs = mockFileSystem("file", byteOut.toByteArray());
        SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path("file"), new Configuration());

        Text key = new Text();
        Text value = new Text();
        assertFalse(reader.next(key, value));
    }
}
