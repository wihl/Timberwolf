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
