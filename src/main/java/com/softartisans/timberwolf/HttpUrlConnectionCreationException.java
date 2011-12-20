package com.softartisans.timberwolf;

/** Exception that can be thrown by HttpUrlConnectionFactories. */
public class HttpUrlConnectionCreationException extends Exception
{
    public HttpUrlConnectionCreationException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
