package com.ripariandata.timberwolf.conf4j;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;

import java.lang.reflect.Field;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;

public class ConfigFileParser
{
    private Map<String, FieldSetter> fields = new HashMap<String, FieldSetter>();

    public ConfigFileParser(Object bean)
    {
        for (Class c = bean.getClass(); c != null; c = c.getSuperclass())
        {
            for (Field f : c.getDeclaredFields())
            {
                ConfigEntry entry = f.getAnnotation(ConfigEntry.class);
                if (entry != null)
                {
                    fields.put(entry.name(), new FieldSetter(bean, f));
                }
            }
        }
    }

    public void parseConfigFile(String configFile) throws ConfigFileException
    {
        File f = new File(configFile);
        if (!f.exists())
        {
            throw new ConfigFileMissingException(configFile);
        }

        Configuration config;
        try
        {
            config = new PropertiesConfiguration(configFile);
        }
        catch (ConfigurationException e)
        {
            throw new ConfigFileException("There was an error loading the configuration file at " + configFile, e);
        }
        parseConfiguration(config);
    }

    public void parseConfiguration(Configuration config)
    {
        Iterator<String> keys = config.getKeys();
        while (keys.hasNext())
        {
            String key = keys.next();
            if (fields.containsKey(key))
            {
                fields.get(key).set(config.getProperty(key));
            }
        }
    }
}
