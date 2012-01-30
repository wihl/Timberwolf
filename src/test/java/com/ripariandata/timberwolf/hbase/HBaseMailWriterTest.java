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
package com.ripariandata.timberwolf.hbase;

import com.ripariandata.timberwolf.MailWriter;
import com.ripariandata.timberwolf.MailboxItem;
import com.ripariandata.timberwolf.MockHTable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for simple App.
 */
public class HBaseMailWriterTest
{
    private Logger logger = LoggerFactory.getLogger(HBaseMailWriter.class);

    /**
     * Creates a mock MailboxItem given a dictionary of headers as keys with the header values as values.
     * @param mailboxItemDescription The above-mentioned dictionary.
     * @return A mock MailboxItem.
     */
    private MailboxItem mockMailboxItem(final Dictionary<String, String> mailboxItemDescription)
    {
        MailboxItem mailboxItem = mock(MailboxItem.class);

        Enumeration<String> keys = mailboxItemDescription.keys();
        String[] headers = new String[mailboxItemDescription.size()];
        headers = Collections.list(keys).toArray(headers);

        when(mailboxItem.getHeaderKeys()).thenReturn(headers);

        for (String header : headers)
        {
            String value = mailboxItemDescription.get(header);

            when(mailboxItem.getHeader(header)).thenReturn(value);
        }

        return mailboxItem;
    }

    /**
     * Generates a dictionary which is suitable for creating a mock MailboxItem.
     * @return A dictionary of MailboxItem-suitable headers and values.
     */
    private Dictionary<String, String> generateMailboxItemDescription()
    {
        Dictionary<String, String> mailboxItemDescription = new Hashtable<String, String>();
        mailboxItemDescription.put("header", "a lonesome value");
        mailboxItemDescription.put("anotherheader", "another value");

        return mailboxItemDescription;
    }

    /**
     * Asserts that all headers and values for a given dictionary are equal to the values present from
     * get calls to an HTableInterface.
     * @param mailTable The HTableInterface to query.
     * @param mailboxItemDescription A description of all the headers and values to query.
     * @param columnFamily The column family for our headers in table.
     * @param rowKey The specific rowKey for this description in the table.
     */
    private void assertMailboxItemDescription(final HTableInterface mailTable,
                                              final Dictionary<String, String> mailboxItemDescription,
                                              final String columnFamily,
                                              final String rowKey)
    {
        Enumeration<String> headers = mailboxItemDescription.keys();

        while (headers.hasMoreElements())
        {
            String header = headers.nextElement();
            String value = mailboxItemDescription.get(header);

            Get get = new Get(Bytes.toBytes(rowKey));

            try
            {
                Result result = mailTable.get(get);
                byte[] valueBytes = result.getValue(Bytes.toBytes(columnFamily), Bytes.toBytes(header));
                Assert.assertEquals(value, Bytes.toString(valueBytes));

            }
            catch (IOException e)
            {
                logger.error("Error during get query.");
            }
        }
    }

    /**
     * Tests that the writer has written the appropriate values from a mock MailboxItem into a
     * mock HTableInterface.
     */
    @Test
    public void testWrite()
    {
        MockHTable mockHTable = MockHTable.create();

        String arbitraryFamily = "columnFamily";
        String arbitraryHeader = "header";

        Dictionary<String, String> mailboxItemDescription = generateMailboxItemDescription();
        MailboxItem mail = mockMailboxItem(mailboxItemDescription);

        List<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mail);

        HBaseTable table = new HBaseTable(mockHTable);

        MailWriter writer = HBaseMailWriter.create(table, arbitraryHeader, arbitraryFamily);

        writer.write(mails);

        assertMailboxItemDescription(mockHTable, mailboxItemDescription, arbitraryFamily,
                mail.getHeader(arbitraryHeader));
    }

    /**
     * Test some of the interfaces the mail writer interacts with.
     */
    @Test
    public void testInterfaces()
    {
        HBaseManager hbase = new HBaseManager();
        MockHTable mockHTable = MockHTable.create("defaultTableName");
        String tableName = Bytes.toString(mockHTable.getTableName());

        HBaseTable table = new HBaseTable(mockHTable);
        Assert.assertEquals(tableName, table.getName());
        hbase.addTable(table);

        IHBaseTable managerTable = hbase.getTable(tableName);
        Assert.assertEquals(table, managerTable);
    }
}
