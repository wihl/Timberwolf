package com.softartisans.timberwolf;



import junit.framework.Assert;
import junit.framework.TestCase;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;
import org.apache.axis2.util.IOUtils;
import org.apache.xmlbeans.impl.schema.FileResourceLoader;
import org.junit.After;
import org.junit.Before;

import java.io.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

class SmockBase
{

    private static Queue<Communication> communications;
    private String path;

    public SmockBase()
    {
        path = '/' + getClass().getCanonicalName().replace('.','/') + '/';
    }

    @Before
    public void initialize()
    {
        try
        {
            communications = new ArrayDeque<Communication>();
            ConfigurationContext configurationContext =
                ConfigurationContextFactory.
                        createConfigurationContextFromFileSystem(null, null);
            HashMap<String, TransportOutDescription> transportsOut =
                configurationContext.getAxisConfiguration().getTransportsOut();
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
            throw new IllegalStateException(
                    "Can not set ConfigurationContext.", e);
        }
    }

    protected Resource fromFile(String filename)
    {
        return new FileResource(path + filename);
    }

    protected static ResponseAction expect(Resource resource)
    {
        Communication communication = new Communication();
        communication.setRequest(resource);
        SmockBase.communications.add(communication);
        return new ResponseAction(communication);
    }

    @After
    protected static void verify()
    {
        if (!communications.isEmpty())
        {
            Assert.fail("Expected " + communications.size() +
                    " more calls than happened");
        }
    }

    protected static interface Resource
    {
        InputStream getStream();
    }

    private class FileResource implements Resource
    {

        private String filename;

        public FileResource(String filename)
        {
            this.filename = filename;
        }

        @Override
        public InputStream getStream()
        {
            return SmockBase.class.getResourceAsStream(filename);
        }
    }

    protected static class ResponseAction {


        private Communication communication;

        public ResponseAction(Communication communication) {
            this.communication = communication;
        }

        public void andRespond(Resource resource) {
            communication.setResponse(resource);
        }
    }

    private static class Communication {

        private Resource request;
        private Resource response;
        private ByteArrayOutputStream output;

        public void setRequest(Resource resource) {
            this.request = resource;
        }

        public void setResponse(Resource resource) {
            this.response = resource;
        }

        public OutputStream getOutputStream()
        {
            output = new ByteArrayOutputStream();
            return output;
        }

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
                    Assert.fail("Could not find response resource: " + response);
                }
                return returnValue;
            }
        }
    }

    private static class MockTransportSender extends AbstractHandler implements TransportSender {

        public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
            Communication communication = communications.remove();
            TransportUtils.writeMessage(msgContext, communication.getOutputStream());
            communication.assertRequest();
            msgContext.setProperty(MessageContext.TRANSPORT_IN, communication.getInputStream());
            TransportUtils.setResponseWritten(msgContext, true);
            return InvocationResponse.CONTINUE;
        }

        public void cleanup(MessageContext msgContext) throws AxisFault {
        }

        public void init(ConfigurationContext configurationContext, TransportOutDescription transportOut) throws AxisFault {
        }

        public void stop() {
        }
    }

}