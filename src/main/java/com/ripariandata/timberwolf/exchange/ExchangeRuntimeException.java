package com.ripariandata.timberwolf.exchange;

/** Non-checked exception thrown by ExchangeMailStore iterator. */
public class ExchangeRuntimeException extends RuntimeException
{
    public ExchangeRuntimeException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
