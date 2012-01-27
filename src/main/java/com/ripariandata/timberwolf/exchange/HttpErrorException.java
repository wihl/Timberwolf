package com.ripariandata.timberwolf.exchange;

/**
 * Exception that gets thrown when an HTTP request returns
 * an error (non-200) response.
 */
public class HttpErrorException extends Exception
{
    private int errorCode;

    public HttpErrorException(final int error)
    {
        super("There was an HTTP " + error + " error while sending a request.");
        errorCode = error;
    }

    public int getErrorCode()
    {
        return errorCode;
    }
}
