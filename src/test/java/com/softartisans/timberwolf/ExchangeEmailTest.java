package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.types.MessageType;

import java.util.Calendar;

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

    public void testTimeSent() {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetDateTimeSent()).thenReturn(true);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2011, 12, 7, 12, 55);
        when(mockedMessage.getDateTimeSent()).thenReturn(calendar);

        Email mail = new ExchangeEmail(mockedMessage);
        Calendar expected = Calendar.getInstance();
        expected.set(2011, 12, 7, 12, 55);
        assertEquals(mail.getTimeSent(), expected.getTime());

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetDateTimeSent()).thenReturn(false);
        when(mockedMessage.isSetDateTimeReceived()).thenReturn(true);
        when(mockedMessage.getDateTimeReceived()).thenReturn(calendar);

        mail = new ExchangeEmail(mockedMessage);
        assertEquals(mail.getTimeSent(), expected.getTime());

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetDateTimeSent()).thenReturn(false);
        when(mockedMessage.isSetDateTimeReceived()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertNull(mail.getTimeSent());
    }
}
