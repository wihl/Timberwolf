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

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests various aspects of a ConsoleMailWriter.
 */
public class ConsoleMailWriterTest extends ConsoleTestBase
{
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
}
