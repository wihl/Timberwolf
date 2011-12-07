package com.softartisans.timberwolf;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.*;

import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */
public class HBaseMailWriterTest
        extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HBaseMailWriterTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( HBaseMailWriterTest.class );
    }
    
    private MailboxItem mockMailboxItem(Dictionary<String, String> mailboxItemDescriptions)
    {
        MailboxItem mailboxItem = mock(MailboxItem.class);

        Enumeration<String> keys = mailboxItemDescriptions.keys();
        while(keys.hasMoreElements())
        {
            String header = keys.nextElement();
            String value = mailboxItemDescriptions.get(header);


        }

        return mailboxItem;
    }

    public void testWrite()
    {
        MockHTable mockHTable = MockHTable.create();
        MailboxItem mailboxItem = mock(MailboxItem.class);
        
        String arbitraryFamily = "columnFamily";
        String arbitraryHeader = "header";
        String arbitraryValue = "a lonesome value";

        when(mailboxItem.getHeaderKeys()).thenReturn(new String[]{ arbitraryHeader });
        when(mailboxItem.getHeader(arbitraryHeader)).thenReturn(arbitraryValue);

        List<MailboxItem> mails = new ArrayList<MailboxItem>();
        mails.add(mailboxItem);
        
        HBaseMailWriter writer = new HBaseMailWriter(mockHTable, arbitraryHeader, arbitraryFamily);
        
        writer.write(mails.iterator());

        Get get = new Get(Bytes.toBytes(arbitraryValue));
        
        try {
            Result result = mockHTable.get(get);
            byte[] value = result.getValue(Bytes.toBytes(arbitraryFamily), Bytes.toBytes(arbitraryHeader));
            Assert.assertEquals(arbitraryValue,Bytes.toString(value));
        }
        catch (IOException e)
        {
            // TODO: Log error.
        }
        
    }

}
