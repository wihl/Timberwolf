package com.softartisans.timberwolf.smock;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;

import java.util.Queue;

/**
 * The class handed to axis as the TransportSender, which actually just
 * asserts the request is correct and returns the mock response.
 */
class MockTransportSender
        extends AbstractHandler
        implements TransportSender
{

    private Queue<Communication> communications;

    public MockTransportSender(
            Queue<Communication> communications)
    {
        this.communications = communications;
    }

    public InvocationResponse invoke(MessageContext msgContext)
            throws AxisFault
    {
        Communication communication = communications.remove();
        TransportUtils.writeMessage(msgContext,
                                    communication.getOutputStream());
        communication.assertRequest();
        msgContext.setProperty(MessageContext.TRANSPORT_IN,
                               communication.getInputStream());
        TransportUtils.setResponseWritten(msgContext, true);
        return InvocationResponse.CONTINUE;
    }

    public void cleanup(MessageContext msgContext) throws AxisFault
    {
    }

    public void init(ConfigurationContext configurationContext,
                     TransportOutDescription transportOut) throws AxisFault
    {
    }

    public void stop()
    {
    }
}
