package com.softartisans.timberwolf;

import com.softartisans.timberwolf.hbase.HBaseTable;
import com.softartisans.timberwolf.hbase.IHBaseTable;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */
public class HBaseTableTest
        extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HBaseTableTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( HBaseTableTest.class );
    }

    /**
     * Default constructor test.
     */
    public void testCreate()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
    }

    /**
     * Name test.
     */
    public void testName()
    {
        String name = "defaultTableName";
        IHBaseTable table = new HBaseTable(MockHTable.create(name));
        Assert.assertEquals(name,table.getName());
    }

    /**
     * Put test.
     */
    public void testPut()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
        Put put = mock(Put.class);
        table.put(put);
    }

    /**
     * Multiple puts test.
     */
    public void testPuts()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
        for (int i = 0; i < 10; i++)
        {
            Put put = mock(Put.class);
            table.put(put);
        }
    }

    /**
     * Just flush test.
     */
    public void testFlush()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
        table.flush();
    }

    /**
     * Flush with puts.
     */
    public void testFlushWithPuts()
    {
        IHBaseTable table = new HBaseTable(MockHTable.create());
        for (int i = 0; i < 10; i++)
        {
            // We currently need at least a row here or a null pointer exception
            // is thrown later in MockHTable.
            Put put = new Put(Bytes.toBytes("dummyRow"));
            table.put(put);
        }
        table.flush();
    }

}

