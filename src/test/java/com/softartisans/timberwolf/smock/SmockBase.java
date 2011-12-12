package com.softartisans.timberwolf.smock;


import junit.framework.Assert;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.util.IOUtils;
import org.junit.After;
import org.junit.Before;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
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
                    tod.setSender(new MockTransportSender());
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

    /**
     * This is the glue that ties expectation and responses together
     */
    private static class Communication
    {

        private Resource request;
        private Resource response;
        private ByteArrayOutputStream output;

        public void setRequest(Resource resource)
        {
            this.request = resource;
        }

        public void setResponse(Resource resource)
        {
            this.response = resource;
        }

        /** Get the stream to which axis should write the request. */
        public OutputStream getOutputStream()
        {
            output = new ByteArrayOutputStream();
            return output;
        }

        /**
         * Assert that the request axis wrote is the same as the one passed
         * into expect.
         */
        public void assertRequest()
        {
            InputStream input = null;
            ByteArrayOutputStream expected = null;
            try
            {
                input = request.getStream();
                if (input == null)
                {
                    Assert.fail("Could not find request resource: " + request);
                }
                expected = new ByteArrayOutputStream();
                IOUtils.copy(input, expected, false);
                Assert.assertEquals(expected.toString("UTF-8"),
                                    output.toString("UTF-8"));
            }
            catch (UnsupportedEncodingException e)
            {
                e.printStackTrace();
                Assert.fail(e.toString());
            }
            catch (IOException e)
            {
                e.printStackTrace();
                Assert.fail(e.toString());
            }
            finally
            {
                safeClose(input);
                safeClose(expected);
            }
        }

        /** helper method to make assertRequest cleaner, closes the Closeable */
        private void safeClose(Closeable input)
        {
            if (input != null)
            {
                try
                {
                    input.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                    Assert.fail(e.toString());
                }
            }
        }

        /** Get the stream that axis should read the mocked response from */
        public InputStream getInputStream()
        {
            if (response == null)
            {
                return new ByteArrayInputStream(new byte[0]);
            }
            else
            {
                InputStream returnValue = response.getStream();
                if (returnValue == null)
                {
                    Assert.fail("Could not find response resource: "
                                + response);
                }
                return returnValue;
            }
        }
    }

    /**
     * The class handed to axis as the TransportSender, which actually just
     * asserts the request is correct and returns the mock response.
     */
    private class MockTransportSender
            extends AbstractHandler
            implements TransportSender
    {

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

}