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
package com.ripariandata.timberwolf.conf4j;

import java.util.Vector;

import org.apache.commons.configuration.Configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Test for the ConfigFileParser that we use to read our configuration. */
public class ConfigFileParserTest
{
    private Configuration mockConfiguration(final Object... settings)
    {
        if ((settings.length % 2) != 0)
        {
            return null;
        }

        Configuration config = mock(Configuration.class);

        Vector<String> keys = new Vector<String>();
        for (int i = 0; i < settings.length; i += 2)
        {
            String key = settings[i].toString();
            keys.add(key);
            when(config.getProperty(key)).thenReturn(settings[i + 1]);
        }
        when(config.getKeys()).thenReturn(keys.iterator());

        return config;
    }

    /** Class for testing parsing no config entries on a class. */
    private class TypeWithNoEntries
    {
        private int x = 0;

        public int x()
        {
            return x;
        }
    }

    @Test
    public void testTypeWithNoEntries()
    {
        TypeWithNoEntries target = new TypeWithNoEntries();
        ConfigFileParser parser = new ConfigFileParser(target);
        Configuration mockConfig = mockConfiguration();
        parser.parseConfiguration(mockConfig);

        assertEquals(0, target.x());

        mockConfig = mockConfiguration("wrong.entry", 100);
        parser.parseConfiguration(mockConfig);
        assertEquals(0, target.x());
    }

    /** Class for testing parsing one config entry on a class. */
    private class TypeWithOneEntry
    {
        @ConfigEntry(name = "my.entry")
        private String entry = "";

        public String entry()
        {
            return entry;
        }
    }

    @Test
    public void testTypeWithOneEntry()
    {
        TypeWithOneEntry target = new TypeWithOneEntry();
        ConfigFileParser parser = new ConfigFileParser(target);

        Configuration mockConfig = mockConfiguration();
        parser.parseConfiguration(mockConfig);
        assertEquals("", target.entry());

        mockConfig = mockConfiguration("wrong.entry", "value");
        parser.parseConfiguration(mockConfig);
        assertEquals("", target.entry());

        mockConfig = mockConfiguration("my.entry", "value");
        parser.parseConfiguration(mockConfig);
        assertEquals("value", target.entry());

        mockConfig = mockConfiguration("wrong.entry", "foo",
                                       "my.entry", "bar",
                                       "another.entry", "baz");
        parser.parseConfiguration(mockConfig);
        assertEquals("bar", target.entry());
    }

    /** Class for testing parsing multiple config entries on one class. */
    private class TypeWithManyEntries
    {
        private String notAnEntry = "";

        @ConfigEntry(name = "entry.string")
        private String stringEntry = "";

        @ConfigEntry(name = "entry.int")
        private int intEntry = 0;

        @ConfigEntry(name = "entry.string.two")
        private String stringTwo = "asdf";

        public String notAnEntry()
        {
            return notAnEntry;
        }

        public String stringEntry()
        {
            return stringEntry;
        }

        public int intEntry()
        {
            return intEntry;
        }

        public String stringTwo()
        {
            return stringTwo;
        }
    }

    @Test
    public void testTypeWithManyEntries()
    {
        TypeWithManyEntries target = new TypeWithManyEntries();
        ConfigFileParser parser = new ConfigFileParser(target);

        Configuration mockConfig = mockConfiguration();
        parser.parseConfiguration(mockConfig);

        assertEquals("", target.notAnEntry());
        assertEquals("", target.stringEntry());
        assertEquals(0, target.intEntry());
        assertEquals("asdf", target.stringTwo());

        mockConfig = mockConfiguration("not.an.entry", "qwer",
                                       "entry.string", "string!",
                                       "entry.int", 10,
                                       "entry.string.two", "qwer");
        parser.parseConfiguration(mockConfig);

        assertEquals("", target.notAnEntry());
        assertEquals("string!", target.stringEntry());
        assertEquals(10, target.intEntry());
        assertEquals("qwer", target.stringTwo());

        mockConfig = mockConfiguration("not.an.entry", "qwer",
                                       "entry.string", "string?",
                                       "entry.int", 100);
        parser.parseConfiguration(mockConfig);

        assertEquals("", target.notAnEntry());
        assertEquals("string?", target.stringEntry());
        assertEquals(100, target.intEntry());
        assertEquals("qwer", target.stringTwo());
    }

    @Test
    public void testMissingFile()
    {
        TypeWithNoEntries target = new TypeWithNoEntries();
        ConfigFileParser parser = new ConfigFileParser(target);

        try
        {
            parser.parseConfigFile("not/a/real/file.properties");
            fail("Attempting to parse a file that doesn't exist should throw an exception.");
        }
        catch (ConfigFileMissingException e)
        {
            // Once we get there the test has passed, but checkstyle complains
            // about a block with no statements.
            assertEquals(true, true);
        }
        catch (Exception e)
        {
            fail("Wrong exception was thrown when attempting to parse missing file: " + e.getMessage());
        }
    }

    /** Class for testing the non-overwriting flag. */
    private class TypeThatRespectsDefaults
    {
        @ConfigEntry(name = "overwritable")
        private String overwritable = null;

        @ConfigEntry(name = "non.overwritable", overwriteNonDefault = false)
        private String nonOverwritable = null;

        @ConfigEntry(name = "has.default", overwriteNonDefault = false)
        private String hasDefault = "default";

        public String overwritable()
        {
            return overwritable;
        }

        public String nonOverwritable()
        {
            return nonOverwritable;
        }

        public String hasDefault()
        {
            return hasDefault;
        }
    }

    @Test
    public void testNotOverwriting()
    {
        TypeThatRespectsDefaults target = new TypeThatRespectsDefaults();
        ConfigFileParser parser = new ConfigFileParser(target);

        Configuration mockConfig = mockConfiguration("overwritable", "new data",
                                                     "non.overwritable", "new data",
                                                     "has.default", "non default");
        parser.parseConfiguration(mockConfig);

        assertEquals("new data", target.overwritable());
        assertEquals("new data", target.nonOverwritable());
        assertEquals("non default", target.hasDefault());

        mockConfig = mockConfiguration("overwritable", "newer data",
                                       "non.overwritable", "newer data",
                                       "has.default", "more non default");
        parser.parseConfiguration(mockConfig);

        assertEquals("newer data", target.overwritable());
        assertEquals("new data", target.nonOverwritable());
        assertEquals("non default", target.hasDefault());
    }
}
