package com.ripariandata.timberwolf.exchange;

import org.slf4j.Logger;

/** Non-checked exception thrown by ExchangeMailStore iterator. */
public class ExchangeRuntimeException extends RuntimeException
{
    public ExchangeRuntimeException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    public static ExchangeRuntimeException log(Logger logger, ExchangeRuntimeException e)
    {
        logger.error(e.getMessage());
        logger.debug(e.getMessage(), e);
        return e;
    }
}
