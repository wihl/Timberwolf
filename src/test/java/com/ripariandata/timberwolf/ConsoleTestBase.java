package com.ripariandata.timberwolf;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;

import org.junit.After;
import org.junit.Before;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Base class for console test suites.
 */
public class ConsoleTestBase
{
    private final ByteArrayOutputStream toBeTested = new ByteArrayOutputStream();

    private PrintStream originalOut;

    @Before
    public void setUpStreams()
    {
        originalOut = System.out;
        System.setOut(new PrintStream(toBeTested));
    }

    @After
    public void cleanUpStreams()
    {
        System.setOut(originalOut);
    }


    protected void assertConsoleOutput(final String[] lines) throws IOException
    {
        String consoleOutput = toBeTested.toString();
        String newline = System.getProperty("line.separator");
        assertTrue(consoleOutput.endsWith(newline));

        BufferedReader reader = new BufferedReader(new StringReader(consoleOutput));
        for (String expectedLine : lines)
        {
            String actualLine = reader.readLine();
            assertEquals(expectedLine, actualLine);
        }

        assertNull("More lines in console than expected", reader.readLine());
    }
}
