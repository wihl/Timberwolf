package com.softartisans.timberwolf.hbase;

import org.slf4j.Logger;

/** Non-checked exception thrown by ExchangeMailStore iterator. */
public class HBaseRuntimeException extends RuntimeException
{
    public HBaseRuntimeException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public static HBaseRuntimeException create(final String message, final Throwable cause, final Logger logger)
    {
        logger.error(message);
        return new HBaseRuntimeException(message, cause);
    }
}

