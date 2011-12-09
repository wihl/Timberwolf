package com.softartisans.timberwolf;



import junit.framework.Assert;
import org.apache.axis2.context.MessageContext;
import org.apache.axis2.description.TransportOutDescription;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.apache.axis2.AxisFault;
import org.apache.axis2.handlers.AbstractHandler;
import org.apache.axis2.transport.TransportSender;
import org.apache.axis2.transport.TransportUtils;

import java.io.*;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Queue;

class Smock {

    private static Queue<Communication> communications;

    public static void initialize() {
        try {
            communications = new ArrayDeque<Communication>();
            ConfigurationContext configurationContext =
                ConfigurationContextFactory.createConfigurationContextFromFileSystem(null, null);
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
            throw new IllegalStateException("Can not set ListenerManager.defaultConfigurationContext.",e);
        }
    }

    public static ResponseAction expect(String xml) {
        Communication communication = new Communication();
        communication.setRequest(xml);
        Smock.communications.add(communication);
        return new ResponseAction(communication);
    }

    public static class ResponseAction {


        private Communication communication;

        public ResponseAction(Communication communication) {
            this.communication = communication;
        }

        public void andRespond(String xml) {
            communication.setResponse(xml);
        }
    }

    private static class Communication {

        private String request;
        private String response;
        private ByteArrayOutputStream output;

        public void setRequest(String xml) {
            this.request = xml;
        }

        public void setResponse(String xml) {
            this.response = xml;
        }

        public OutputStream getOutputStream()
        {
            output = new ByteArrayOutputStream();
            return output;
        }

        public void assertRequest()
        {
            try
            {
                Assert.assertEquals(request, output.toString("UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Assert.fail(e.toString());
            }
        }

        public InputStream getInputStream()
        {
            try
            {
                if (response == null)
                {
                    return new ByteArrayInputStream(new byte[0]);
                }
                else
                {
                    return new ByteArrayInputStream(response.getBytes("UTF-8"));
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Assert.fail(e.toString());
            }
            // this line will never be reached but java doesn't know that
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    private static class MockTransportSender extends AbstractHandler implements TransportSender {

        public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
            Communication communication = communications.remove();
            TransportUtils.writeMessage(msgContext, communication.getOutputStream());
            communication.assertRequest();
            //String response = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"                xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"                xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">  <soap:Header>    <t:ServerVersionInfo MajorVersion=\"8\" MinorVersion=\"0\" MajorBuildNumber=\"595\" MinorBuildNumber=\"0\"                          xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" />  </soap:Header>  <soap:Body>    <FindItemResponse xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\"                       xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\"                       xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\">      <m:ResponseMessages>        <m:FindItemResponseMessage ResponseClass=\"Success\">          <m:ResponseCode>NoError</m:ResponseCode>          <m:RootFolder TotalItemsInView=\"10\" IncludesLastItemInRange=\"true\">            <t:Items>              <t:Message>                <t:ItemId Id=\"AS4AUn=\" ChangeKey=\"fsVU4==\" />              </t:Message>              <t:Message>                <t:ItemId Id=\"AS4AUM=\" ChangeKey=\"fsVUA==\" />              </t:Message>            </t:Items>          </m:RootFolder>        </m:FindItemResponseMessage>      </m:ResponseMessages>    </FindItemResponse>  </soap:Body></soap:Envelope>";
            //InputStream inStream = new ByteArrayInputStream(response.getBytes("UTF-8"));
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