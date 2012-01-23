package com.ripariandata.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.ResponseCodeType;
import org.slf4j.Logger;

/** Exception that can be thrown by HttpUrlConnectionFactories. */
public class ServiceCallException extends Exception
{
    /**
     * Reason values are used to indicate which broad class of problem caused a
     * service call to fail.
     */
    public enum Reason
    {
        /** Indicates that an Exchange SOAP response contained an error code. */
        SOAP,
        /** Indicates that the service call could not authenticate with the server. */
        AUTHENTICATION,
        /** The service call failed for reasons not covered by the other options. */
        OTHER
    }

    private Reason errorReason;
    private ResponseCodeType.Enum soapErrorClass;

    public ServiceCallException(final Reason reason, final String message)
    {
        super(message);
        errorReason = reason;
    }

    public ServiceCallException(final Reason reason, final String message, final Throwable cause)
    {
        super(message, cause);
        errorReason = reason;
    }

    public ServiceCallException(final ResponseCodeType.Enum errorClass, final String message)
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

    /**
     * Logs a ServiceCallException to the appropriate logs.
     * @param logger The logger to use for logging.
     * @param e The ServiceCallException to log.
     * @return The ServiceCallException logged.
     */
    public static ServiceCallException log(final Logger logger, final ServiceCallException e)
    {
        logger.error(e.getMessage());
        logger.debug(e.getMessage(), e);
        return e;
    }

}
