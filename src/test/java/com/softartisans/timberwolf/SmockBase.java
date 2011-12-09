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

import java.io.*;
import java.net.URL;
import java.util.ArrayDeque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Queue;

class SmockBase extends TestCase
{

    private static Queue<Communication> communications;
    private String path;

    public SmockBase(String testName)
    {
        super(testName);
        path = '/' + getClass().getCanonicalName().replace('.','/') + '/';
    }

    public void setUp()
    {
        SmockBase.initialize();
    }

    public void tearDown() {
        SmockBase.verify();
    }

    public static void initialize() {
        try {
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
        } catch (AxisFault e) {
            throw new IllegalStateException(
                    "Can not set ConfigurationContext.", e);
        }
    }

    protected Resource fromFile(String filename)
    {
        return new FileResource(path + filename);
    }

    protected static ResponseAction expect(String filename) {
        Communication communication = new Communication();
        communication.setRequest(filename);
        SmockBase.communications.add(communication);
        return new ResponseAction(communication);
    }

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

        public void andRespond(String filename) {
            communication.setResponse(filename);
        }
    }

    private static class Communication {

        private String request;
        private String response;
        private ByteArrayOutputStream output;

        public void setRequest(String filename) {
            this.request = filename;
        }

        public void setResponse(String filename) {
            this.response = filename;
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
                input = SmockBase.class.getResourceAsStream(request);
                if (input == null)
                {
                    System.out.println("Existing resources: ");
                    for (Enumeration<URL> e = SmockBase.class.getClassLoader().getResources("request1.xml"); e.hasMoreElements();)
                    {
                        System.out.println(e.nextElement());
                    }
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
                if (expected != null)
                {
                    try
                    {
                        expected.close();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                        Assert.fail(e.toString());
                    }
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
                InputStream returnValue = SmockBase.class.getResourceAsStream(response);
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