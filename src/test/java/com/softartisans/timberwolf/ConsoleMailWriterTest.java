package com.softartisans.timberwolf;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class ConsoleMailWriterTest
{
    private final ByteArrayOutputStream toBeTested = new ByteArrayOutputStream();
    
    @Before
    public void setUpStreams()
    {
        System.setOut(new PrintStream(toBeTested));
    }
    
    @After
    public void cleanUpStreams()
    {
        System.setOut(null);
    }
    
    @Test
    public void testWrite() throws IOException
    {
        MailboxItem item1 = mock(MailboxItem.class);
        when(item1.getHeaderKeys()).thenReturn(new String[] {"Body", "Recipient", "CC", "BCC"});
        when(item1.getHeader("Body")).thenReturn("Body text");
        when(item1.getHeader("Recipient")).thenReturn("to@me.com");
        when(item1.getHeader("CC")).thenReturn("fake@names3.com");
        when(item1.getHeader("BCC")).thenReturn("fake@names.com;fake2@names.com");
        
        MailboxItem item2 = mock(MailboxItem.class);
        when(item2.getHeaderKeys()).thenReturn(new String[]{"Room", "Time", "Importance"});
        when(item2.getHeader("Room")).thenReturn("Body text");
        when(item2.getHeader("Time")).thenReturn("Tuesday December 5th");
        when(item2.getHeader("Importance")).thenReturn("Low");
        
        ArrayList<MailboxItem> items = new ArrayList<MailboxItem>();
        items.add(item1);
        items.add(item2);
        
        ConsoleMailWriter writer = new ConsoleMailWriter();
        writer.write(items);
        
        String[] lines = new String[]{
                "===========================================================",
                "Body: Body text",
                "Recipient: to@me.com",
                "CC: fake@names3.com",
                "BCC: fake@names.com;fake2@names.com",
                "===========================================================",
                "Room: Body text",
                "Time: Tuesday December 5th",
                "Importance: Low"
        };
        
        assertConsoleOutput(lines);
    }

    private void assertConsoleOutput(String[] lines) throws IOException
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
