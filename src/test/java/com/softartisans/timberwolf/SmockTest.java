package com.softartisans.timberwolf;

import com.softartisans.timberwolf.smock.SmockBase;
import junit.framework.Assert;

import org.apache.axis2.AxisFault;
import com.softartisans.timberwolf.exchangeservice.ExchangeServiceStub;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemDocument;
import com.microsoft.schemas.exchange.services._2006.messages.FindItemResponseDocument;
import org.junit.Before;
import org.junit.Test;

import java.rmi.RemoteException;

/** Test suite for testing SmockBase */
public class SmockTest extends SmockBase
{
    private ExchangeServiceStub stub;

    /**
     * Initializes stub to be an ExchangeServiceStub that doesn't point to an
     * actual server
     */
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
        expect(fromFile("request1.xml"))
                .andRespond(fromFile("response1.xml"));
        FindItemDocument fid = FindItemDocument.Factory.newInstance();
        FindItemResponseDocument fir = stub.findItem(fid,
                                                     null, null, null, null);
    }

}