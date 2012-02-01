package com.ripariandata.timberwolf.conf4j;

/** Thrown when ConfigFileParser attempts to parse a configuration file that doesn't exist. */
public class ConfigFileMissingException extends ConfigFileException
{
    public ConfigFileMissingException(final String file)
    {
        super("Could not locate configuration file " + file, null);
    }
}
