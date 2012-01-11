package com.softartisans.timberwolf.services;

/**
 * This or a derivative of this is thrown should an error occur fetching
 * a complete list of principals from whatever derives PrincipalFetcher.
 */
public class PrincipalFetchException extends Exception
{
    public PrincipalFetchException(final Exception innerException)
    {
        super(innerException);
    }

    public PrincipalFetchException(final String message,
                                   final Exception innerException)
    {
        super(message, innerException);
    }
}
