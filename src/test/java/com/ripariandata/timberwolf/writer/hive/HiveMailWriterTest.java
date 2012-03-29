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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.util.ArrayList;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;

import org.junit.Test;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.matches;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**Tests the HiveMailWriter.*/
public class HiveMailWriterTest
{
    @Test
    public void testClosingClosesBothThings() throws SQLException, IOException
    {
        Connection hive = mock(Connection.class);
        FileSystem hdfs = mock(FileSystem.class);

        HiveMailWriter writer = new HiveMailWriter(hdfs, hive, "table");
        writer.close();

        verify(hive).close();
        verify(hdfs).close();
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testWritingToNewTable() throws SQLException, IOException
    {
        Connection hive = mock(Connection.class);
        FileSystem hdfs = mock(FileSystem.class);

        PreparedStatement showStmt = createMockTableExistsResponse(hive, false);

        Statement createStmt = mock(Statement.class);
        when(hive.createStatement()).thenReturn(createStmt);

        PreparedStatement loadStmt = mock(PreparedStatement.class);
        when(hive.prepareStatement("load data inpath ? into table new_table")).thenReturn(loadStmt);

        when(hdfs.exists(eq(new Path("/tmp/timberwolf")))).thenReturn(false);
        when(hdfs.create(any(Path.class))).thenReturn(new FSDataOutputStream(new ByteArrayOutputStream()));

        HiveMailWriter writer = new HiveMailWriter(hdfs, hive, "new_table");
        writer.write(new ArrayList<MailboxItem>());

        verify(showStmt).setString(1, "new_table");
        String s = "create table new_table \\([^)]+\\)"
            + " row format delimited fields terminated by '\\\\037'"
            + " stored as sequencefile";
        verify(createStmt).executeQuery(matches(s));
        verify(loadStmt).setString(eq(1), startsWith("/tmp/timberwolf/"));
        verify(loadStmt).executeQuery();

        verify(hdfs).mkdirs(eq(new Path("/tmp/timberwolf")));
        verify(hdfs).delete(any(Path.class), eq(false));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testWritingToExistingTable() throws SQLException, IOException
    {
        Connection hive = mock(Connection.class);
        FileSystem hdfs = mock(FileSystem.class);

        PreparedStatement showStmt = createMockTableExistsResponse(hive, true);

        PreparedStatement loadStmt = mock(PreparedStatement.class);
        when(hive.prepareStatement("load data inpath ? into table new_table")).thenReturn(loadStmt);

        when(hdfs.exists(eq(new Path("/tmp/timberwolf")))).thenReturn(false);
        when(hdfs.create(any(Path.class))).thenReturn(new FSDataOutputStream(new ByteArrayOutputStream()));

        HiveMailWriter writer = new HiveMailWriter(hdfs, hive, "new_table");
        writer.write(new ArrayList<MailboxItem>());

        verify(showStmt).setString(1, "new_table");
        verify(loadStmt).setString(eq(1), startsWith("/tmp/timberwolf/"));
        verify(loadStmt).executeQuery();

        verify(hdfs).mkdirs(eq(new Path("/tmp/timberwolf")));
        verify(hdfs).delete(any(Path.class), eq(false));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testWritingAndFailing() throws SQLException, IOException
    {
        Connection hive = mock(Connection.class);
        FileSystem hdfs = mock(FileSystem.class);

        PreparedStatement showStmt = createMockTableExistsResponse(hive, true);

        PreparedStatement loadStmt = mock(PreparedStatement.class);
        when(hive.prepareStatement("load data inpath ? into table new_table")).thenReturn(loadStmt);
        // Here is the change, we throw an exception instead of returning
        when(loadStmt.executeQuery()).thenThrow(new SQLException("mock failed to load"));

        when(hdfs.exists(eq(new Path("/tmp/timberwolf")))).thenReturn(true);
        when(hdfs.create(any(Path.class))).thenReturn(new FSDataOutputStream(new ByteArrayOutputStream()));

        HiveMailWriter writer = new HiveMailWriter(hdfs, hive, "new_table");
        try
        {
            writer.write(new ArrayList<MailboxItem>());
            fail("The SQLException got swallowed. It should not be swallowed.");
        }
        catch (HiveMailWriterException e)
        {
            // This is what we're testing really.
            // We want to make sure this happens in case of exception.
            verify(hdfs).delete(any(Path.class), eq(false));
        }

        verify(showStmt).setString(1, "new_table");
        verify(loadStmt).setString(eq(1), startsWith("/tmp/timberwolf/"));
    }


    /**
     * When the hive connection is told to prepare "show tables ?",
     * it will return the object returned by this method. This object
     * will return 'tableWillExist' when executed.
     *
     * That question mark gets set by a setString call to the PreparedStatement.
     */
    private static PreparedStatement createMockTableExistsResponse(
            final Connection hive,
            final boolean tableWillExist)
            throws SQLException
    {
        PreparedStatement showStmt = mock(PreparedStatement.class);
        ResultSet showResult = mock(ResultSet.class);
        when(showResult.next()).thenReturn(tableWillExist);
        when(showStmt.executeQuery()).thenReturn(showResult);
        when(hive.prepareStatement("show tables ?")).thenReturn(showStmt);
        return showStmt;
    }

}
