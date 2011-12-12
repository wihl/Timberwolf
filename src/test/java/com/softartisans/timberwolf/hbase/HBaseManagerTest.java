package com.softartisans.timberwolf.hbase;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import static org.mockito.Mockito.*;

/**
 * Unit test for HBaseManager.
 */
public class HBaseManagerTest
        extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public HBaseManagerTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( HBaseManagerTest.class );
    }

    /**
     * Create test.
     */
    public void testCreate()
    {
        HBaseManager hbase = new HBaseManager();
    }

    /**
     * Test that we can simply add.
     */
    public void testAdd()
    {
        HBaseManager hbase = new HBaseManager();
        IHBaseTable table = mock(HBaseTable.class);
        hbase.add(table);
    }

    /**
     * Test that we can get a table, and it is the same
     * as the one put in.
     */
    public void testGetFromAdd()
    {
        String tableName = "defaultTableName";
        HBaseManager hbase = new HBaseManager();
        IHBaseTable table = createNamedTable(tableName);
        hbase.add(table);
        IHBaseTable managerTable = hbase.get(tableName);
        Assert.assertEquals(table, managerTable);
        Assert.assertEquals(tableName, managerTable.getName());
    }

    /**
     * Creates an IHBaseTable with a specific name.
     * @param name The name of the IHBaseTable.
     * @return A IHBaseTable with a specific name.
     */
    private IHBaseTable createNamedTable(String name)
    {
        HBaseTable table = mock(HBaseTable.class);
        when(table.getName()).thenReturn(name);
        return table;
    }
}
