package com.softartisans.timberwolf.smock;


import junit.framework.Assert;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.AxisFault;
import org.junit.After;
import org.junit.Before;

import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

/**
 * A base class for all test suites that need to mock out the webservice.
 * This is for mocking out the actual network calls, not the objects that
 * axis creates.
 * <br />
 * This might have problems if the communications are being created with a
 * custom ConfigurationContext or MessageContext
 */
public class SmockBase
{

    private Queue<Communication> communications;
    private String path;

    /** Create the smock base */
    public SmockBase()
    {
        path = '/' + getClass().getCanonicalName().replace('.','/') + '/';
    }

    /**
     * Sets up queue for expectations and attaches it to the axis2.
     * This must be called before creating the Stub object.
     */
    @Before
    public void initialize()
    {
        try
        {
            communications = new ArrayDeque<Communication>();
            ConfigurationContext configurationContext =
                    ConfigurationContextFactory
                            .createConfigurationContextFromFileSystem(null,
                                                                      null);
            HashMap<String, TransportOutDescription> transportsOut
                    = configurationContext.getAxisConfiguration()
                                          .getTransportsOut();
            for (TransportOutDescription tod: transportsOut.values())
            {
                if (tod!=null)
                {
                    tod.setSender(new MockTransportSender(communications));
                }
            }
            MessageContext messageContext = new MessageContext();
            messageContext.setConfigurationContext(configurationContext);
            MessageContext.setCurrentMessageContext(messageContext);
        }
        catch (AxisFault e)
        {
            throw new IllegalStateException("Can not set ConfigurationContext.",
                    e);
        }
    }

    /**
     * Creates a resources from a file in the test/resources folder
     * @param filename The filename of the resource. This file should be placed
     * in a directory corresponding to the full name of the testing class.
     * For Example, from the class com.softartisans.timberwolf.SmockTest, to
     * include the file at
     * test/resources/com/softartisans/timberwolf/SmockTest/request1.xml
     * "request1.xml" should be passed in
     * @return the resource to be passed to expect or andRespond
     */
    protected Resource fromFile(String filename)
    {
        return new FileResource(path + filename);
    }

    /** Expects a given xml request */
    protected ResponseAction expect(Resource resource)
    {
        Communication communication = new Communication();
        communication.setRequest(resource);
        communications.add(communication);
        return new ResponseAction(communication);
    }

    /** Verify that there weren't more excpectations than actual calls */
    @After
    public void verify()
    {
        if (!communications.isEmpty())
        {
            Assert.fail("Expected " + communications.size()
                        + " more calls than happened");
        }
    }

    /** Simple interface passed to expect or andRespond to get xml content */
    protected static interface Resource
    {
        /** Returns a stream containing the actual request or response */
        InputStream getStream();
    }

    /**
     *  A version of Resource that grabs the resource from a resource in the
     *  classpath, in general this will be in the jar
     */
    private class FileResource implements Resource
    {

        private String filename;

        /** default constructor
         *
         * @param filename the absolute path to the resource
         */
        public FileResource(String filename)
        {
            this.filename = filename;
        }

        /** returns the stream from the file */
        @Override
        public InputStream getStream()
        {
            return SmockBase.class.getResourceAsStream(filename);
        }
    }

    /** The class in charge of mocking out the response */
    protected static class ResponseAction
    {

        private Communication communication;

        private ResponseAction(Communication communication)
        {
            this.communication = communication;
        }

        /**
         * Respond with the given resource
         * @param resource the resource to use as the response to the call
         */
        public void andRespond(Resource resource)
        {
            communication.setResponse(resource);
        }
    }

}