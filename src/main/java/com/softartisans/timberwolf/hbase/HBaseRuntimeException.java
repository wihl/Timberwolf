package com.softartisans.timberwolf.hbase;

/** Non-checked exception thrown by ExchangeMailStore iterator. */
public class HBaseRuntimeException extends RuntimeException
{
    public HBaseRuntimeException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}

