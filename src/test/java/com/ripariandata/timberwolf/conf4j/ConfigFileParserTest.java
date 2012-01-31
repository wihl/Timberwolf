package com.ripariandata.timberwolf.conf4j;

import org.apache.commons.configuration.Configuration;

import org.junit.Test;

import java.util.Vector;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ConfigFileParserTest
{
    private Configuration mockConfiguration(Object... settings)
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

    private class TypeWithNoEntries
    {
        public int x = 0;
    }

    @Test
    public void testTypeWithNoEntries()
    {
        TypeWithNoEntries target = new TypeWithNoEntries();
        ConfigFileParser parser = new ConfigFileParser(target);
        Configuration mockConfig = mockConfiguration();
        parser.parseConfiguration(mockConfig);

        assertEquals(0, target.x);

        mockConfig = mockConfiguration("wrong.entry", 100);
        parser.parseConfiguration(mockConfig);
        assertEquals(0, target.x);
    }

    private class TypeWithOneEntry
    {
        @ConfigEntry(name = "my.entry")
        public String entry = "";
    }

    @Test
    public void testTypeWithOneEntry()
    {
        TypeWithOneEntry target = new TypeWithOneEntry();
        ConfigFileParser parser = new ConfigFileParser(target);

        Configuration mockConfig = mockConfiguration();
        parser.parseConfiguration(mockConfig);
        assertEquals("", target.entry);

        mockConfig = mockConfiguration("wrong.entry", "value");
        parser.parseConfiguration(mockConfig);
        assertEquals("", target.entry);

        mockConfig = mockConfiguration("my.entry", "value");
        parser.parseConfiguration(mockConfig);
        assertEquals("value", target.entry);

        mockConfig = mockConfiguration("wrong.entry", "foo",
                                       "my.entry", "bar",
                                       "another.entry", "baz");
        parser.parseConfiguration(mockConfig);
        assertEquals("bar", target.entry);
    }

    private class TypeWithManyEntries
    {
        public String notAnEntry = "";

        @ConfigEntry(name = "entry.string")
        public String stringEntry = "";

        @ConfigEntry(name = "entry.int")
        public int intEntry = 0;

        @ConfigEntry(name = "entry.string.two")
        public String stringTwo = "asdf";
    }

    @Test
    public void testTypeWithManyEntries()
    {
        TypeWithManyEntries target = new TypeWithManyEntries();
        ConfigFileParser parser = new ConfigFileParser(target);

        Configuration mockConfig = mockConfiguration();
        parser.parseConfiguration(mockConfig);

        assertEquals("", target.notAnEntry);
        assertEquals("", target.stringEntry);
        assertEquals(0, target.intEntry);
        assertEquals("asdf", target.stringTwo);

        mockConfig = mockConfiguration("not.an.entry", "qwer",
                                       "entry.string", "string!",
                                       "entry.int", 10,
                                       "entry.string.two", "qwer");
        parser.parseConfiguration(mockConfig);

        assertEquals("", target.notAnEntry);
        assertEquals("string!", target.stringEntry);
        assertEquals(10, target.intEntry);
        assertEquals("qwer", target.stringTwo);

        mockConfig = mockConfiguration("not.an.entry", "qwer",
                                       "entry.string", "string?",
                                       "entry.int", 100);
        parser.parseConfiguration(mockConfig);

        assertEquals("", target.notAnEntry);
        assertEquals("string?", target.stringEntry);
        assertEquals(100, target.intEntry);
        assertEquals("qwer", target.stringTwo);
    }
}
