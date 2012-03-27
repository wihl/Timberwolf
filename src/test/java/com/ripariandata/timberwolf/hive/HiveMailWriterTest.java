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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.net.URI;
import java.net.URISyntaxException;

import java.sql.SQLException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.DriverManager;

import java.util.ArrayList;
import java.util.UUID;

import org.junit.Test;

import org.apache.commons.lang.StringUtils;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.regex.PatternSyntaxException;

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

        PreparedStatement showStmt = mock(PreparedStatement.class);
        ResultSet showResult = mock(ResultSet.class);
        when(showResult.next()).thenReturn(false);
        when(showStmt.executeQuery()).thenReturn(showResult);
        when(hive.prepareStatement("show tables ?")).thenReturn(showStmt);

        Statement createStmt = mock(Statement.class);
        when(hive.createStatement()).thenReturn(createStmt);

        PreparedStatement loadStmt = mock(PreparedStatement.class);
        when(hive.prepareStatement("load data inpath ? into table new_table")).thenReturn(loadStmt);

        when(hdfs.exists(eq(new Path("/tmp/timberwolf")))).thenReturn(false);
        when(hdfs.create(any(Path.class))).thenReturn(new FSDataOutputStream(new ByteArrayOutputStream()));

        HiveMailWriter writer = new HiveMailWriter(hdfs, hive, "new_table");
        writer.write(new ArrayList<MailboxItem>());

        verify(showStmt).setString(1, "new_table");
        String s = "create table new_table \\([^)]+\\) row format delimited fields terminated by '\\\\037' stored as sequencefile";
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

        PreparedStatement showStmt = mock(PreparedStatement.class);
        ResultSet showResult = mock(ResultSet.class);
        when(showResult.next()).thenReturn(true);
        when(showStmt.executeQuery()).thenReturn(showResult);
        when(hive.prepareStatement("show tables ?")).thenReturn(showStmt);

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
}
