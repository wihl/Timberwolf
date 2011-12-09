package com.softartisans.timberwolf;

import junit.framework.Assert;

import org.apache.axis2.AxisFault;
import com.softartisans.timberwolf.exchangeservice.ExchangeServiceStub;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseDocument;
import com.microsoft.schemas.exchange.services._2006.types.ExchangeImpersonationDocument;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;

/**
 * Unit test for testing SmockBase
 */
public class SmockTest extends SmockBase
{
    private ExchangeServiceStub stub;

    @Before
    public void setUp() {
        try {
            stub = new ExchangeServiceStub("http://glue:8080/Exchange");
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
            Assert.fail("Could not create ExchangeServiceStub: " + axisFault);
        }
    }

    @Test
    public void testSmock() throws RemoteException {
        SmockBase.expect(fromFile("request1.xml")).
            andRespond(fromFile("response1.xml"));
        FindItemDocument fid = FindItemDocument.Factory.newInstance();
        FindItemResponseDocument fir = stub.findItem(fid,
                null, null, null, null);
    }

}