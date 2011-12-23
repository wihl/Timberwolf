package com.softartisans.timberwolf.exchange;

/** Exception that can be thrown by HttpUrlConnectionFactories. */
public class HttpUrlConnectionCreationException extends Exception
{
    public HttpUrlConnectionCreationException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
