package com.ripariandata.timberwolf.conf4j;

import java.util.Map;
import java.util.HashMap;

public class ConfigFileParser
{
    private Map<String, FieldSetter> fields = new Map<String, FieldSetter>();

    public ConfigFileParser(Object bean)
    {
        for (Field f : bean.getClass().getDeclaredFields())
        {
            ConfigEntry entry = f.getAnnotation(ConfigEntry.class);
            if (entry != null)
            {
                fields.put(entry.name(), new FieldSetter(bean, f));
            }
        }
    }
}
