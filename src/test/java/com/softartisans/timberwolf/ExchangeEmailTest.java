package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.types.MessageType;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import static org.mockito.Mockito.*;

public class ExchangeEmailTest extends TestCase {
    public ExchangeEmailTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ExchangeEmailTest.class);
    }

    public void testSubject() {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetSubject()).thenReturn(true);
        when(mockedMessage.getSubject()).thenReturn("Test Email");
        
        Email mail = new ExchangeEmail(mockedMessage);
        assertEquals(mail.getSubject(), "Test Email");

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetSubject()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertEquals(mail.getSubject(), "");
    }
}
