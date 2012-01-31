package com.ripariandata.timberwolf.conf4j;

public class ConfigFileMissingException extends ConfigFileException
{
    public ConfigFileMissingException(String file)
    {
        super("Could not locate configuration file " + file, null);
    }
}
