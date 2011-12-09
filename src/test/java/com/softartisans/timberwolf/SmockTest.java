package com.softartisans.timberwolf;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import net.javacrumbs.smock.extended.client.connection.MockWebServiceServer;
import org.apache.axis2.AxisFault;
import com.softartisans.timberwolf.exchangeservice.ExchangeServiceStub;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseDocument;
import com.microsoft.schemas.exchange.services._2006.types.ExchangeImpersonationDocument;
import com.microsoft.schemas.exchange.services._2006.types.MailboxCultureDocument;
import com.microsoft.schemas.exchange.services._2006.types.RequestServerVersionDocument;
import com.microsoft.schemas.exchange.services._2006.types.TimeZoneContextDocument;

import java.rmi.RemoteException;

/**
 * Unit test for simple App.
 */
public class SmockTest
    extends TestCase
{
    private static final String resourcePrefix = "com/softartisans/timberwolf/SmockTest/";
    private ExchangeServiceStub stub;


    public void setUp() {
        Smock.initialize();
        // client has to be created after createServer was called
        try {
            stub = new ExchangeServiceStub("http://glue:8080/axis2/services/Exchange");
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
            Assert.fail("Could not create ExchangeServiceStub: " + axisFault);
        }
    }

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public SmockTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( SmockTest.class );
    }

    public void testSmock() throws RemoteException {
        // mockServer.expect(message(resourcePrefix + "request1.xml")).
        //    andRespond(withMessage(resourcePrefix + "response1.xml"));
        Smock.expect("foo").andRespond("<?xml version=\"1.0\" encoding=\"utf-8\"?>\n<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"\n               xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n               xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n    <soap:Header>\n        <t:ServerVersionInfo MajorVersion=\"8\" MinorVersion=\"0\" MajorBuildNumber=\"595\" MinorBuildNumber=\"0\"\n                             xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\" />\n    </soap:Header>\n    <soap:Body>\n        <FindItemResponse xmlns:m=\"http://schemas.microsoft.com/exchange/services/2006/messages\"\n                          xmlns:t=\"http://schemas.microsoft.com/exchange/services/2006/types\"\n                          xmlns=\"http://schemas.microsoft.com/exchange/services/2006/messages\">\n            <m:ResponseMessages>\n                <m:FindItemResponseMessage ResponseClass=\"Success\">\n                <m:ResponseCode>NoError</m:ResponseCode>\n                    <m:RootFolder TotalItemsInView=\"10\" IncludesLastItemInRange=\"true\">\n                        <t:Items>\n                            <t:Message>\n                                <t:ItemId Id=\"AS4AUn=\" ChangeKey=\"fsVU4==\" />\n                            </t:Message>\n                            <t:Message>\n                                <t:ItemId Id=\"AS4AUM=\" ChangeKey=\"fsVUA==\" />\n                            </t:Message>\n                        </t:Items>\n                    </m:RootFolder>\n                </m:FindItemResponseMessage>\n            </m:ResponseMessages>\n        </FindItemResponse>\n    </soap:Body>\n</soap:Envelope>");
        FindItemDocument fid = FindItemDocument.Factory.newInstance();
        ExchangeImpersonationDocument eid = ExchangeImpersonationDocument.Factory.newInstance();
        MailboxCultureDocument mcd = MailboxCultureDocument.Factory.newInstance();
        RequestServerVersionDocument rsvd = RequestServerVersionDocument.Factory.newInstance();
        TimeZoneContextDocument tzcd = TimeZoneContextDocument.Factory.newInstance();
        FindItemResponseDocument fir = stub.findItem(fid, null, null, null, null);
//        mockServer.verify();
        Assert.fail();
    }

    public void tearDown() {
        //        mockServer.verify();
    }

}