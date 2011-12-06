package com.softartisans.timberwolf;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */
public class AppTest 
    extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( AppTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testApp()
    {
        assertTrue( true );
    }
    
    public void testMock()
    {
        Email mockEmail = mock(Email.class);
        when(mockEmail.getBody()).thenReturn("rufio");
        assertEquals(mockEmail.getBody(),"rufio");
    }
}
