package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;

/** Exception that can be thrown by HttpUrlConnectionFactories. */
public class ServiceCallException extends Exception
{
    public enum Reason
    {
        SOAP, AUTHENTICATION, OTHER
    }

    private Reason errorReason;
    private ResponseCodeType.Enum soapErrorClass;

    public ServiceCallException(Reason reason, String message)
    {
        super(message);
        errorReason = reason;
    }

    public ServiceCallException(Reason reason, String message, Throwable cause)
    {
        super(message, cause);
        errorReason = reason;
    }

    public ServiceCallException(ResponseCodeType.Enum errorClass, String message)
    {
        super(message);
        errorReason = Reason.SOAP;
        soapErrorClass = errorClass;
    }

    /** Gets the reason that the service call failed. */
    public Reason getReason()
    {
        return errorReason;
    }

    /** 
     * Gets the Exchange response code that caused the error, if appropriate.
     *
     * Returns null if getReason() does not return Reason.SOAP.
     */
    public ResponseCodeType.Enum getSoapError()
    {
        if (errorReason == Reason.SOAP)
        {
            return soapErrorClass;
        }
        return null;        
    }
}
