package com.softartisans.timberwolf.exchange;

/**
 * This or a derivitive of this is thrown should an error occur fetching
 * a complete list of principals from whatever derives PrincipalFetcher.
 */
public class PrincipalFetchException extends Exception
{
    public PrincipalFetchException(final Exception innerException)
    {
        super(innerException);
    }

}
