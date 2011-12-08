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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

class Smock {


    public static void initialize() {
        try {
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

    private static class MockTransportSender extends AbstractHandler implements TransportSender {

        public InvocationResponse invoke(MessageContext msgContext) throws AxisFault {
            TransportUtils.writeMessage(msgContext, System.out);
            try {
                String response = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"                xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"                xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">  <soap:Header>    <t:ServerVersionInfo MajorVersion=\"8\" MinorVersion=\"0\" MajorBuildNumber=\"595\" MinorBuildNumber=\"0\"                          xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" />  </soap:Header>  <soap:Body>    <FindItemResponse xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\"                       xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\"                       xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\">      <m:ResponseMessages>        <m:FindItemResponseMessage ResponseClass=\"Success\">          <m:ResponseCode>NoError</m:ResponseCode>          <m:RootFolder TotalItemsInView=\"10\" IncludesLastItemInRange=\"true\">            <t:Items>              <t:Message>                <t:ItemId Id=\"AS4AUn=\" ChangeKey=\"fsVU4==\" />              </t:Message>              <t:Message>                <t:ItemId Id=\"AS4AUM=\" ChangeKey=\"fsVUA==\" />              </t:Message>            </t:Items>          </m:RootFolder>        </m:FindItemResponseMessage>      </m:ResponseMessages>    </FindItemResponse>  </soap:Body></soap:Envelope>";
                InputStream inStream = new ByteArrayInputStream(response.getBytes("UTF-8"));
                msgContext.setProperty(MessageContext.TRANSPORT_IN, inStream);
                TransportUtils.setResponseWritten(msgContext, true);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Assert.fail(e.toString());
            }
            return InvocationResponse.CONTINUE;
        }

        public void cleanup(MessageContext msgContext) throws AxisFault {
        }

        public void init(ConfigurationContext confContext, TransportOutDescription transportOut) throws AxisFault {
        }

        public void stop() {
        }
    }
}