package com.ripariandata.timberwolf.conf4j;

/** Thrown when ConfigFileParser fails to load a configuration file. */
public class ConfigFileException extends Exception
{
    public ConfigFileException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
