package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.types.MessageType;
import com.microsoft.schemas.exchange.services._2006.types.BodyType;

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
        
        MailboxItem mail = new ExchangeEmail(mockedMessage);/*
        assertEquals(mail.getSubject(), "Test Email");

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetSubject()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertEquals(mail.getSubject(), "");*/
    }

    public void testTimeSent() {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetDateTimeSent()).thenReturn(true);
        Calendar calendar = Calendar.getInstance();
        calendar.set(2011, 12, 7, 12, 55);
        when(mockedMessage.getDateTimeSent()).thenReturn(calendar);

        MailboxItem mail = new ExchangeEmail(mockedMessage);/*
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
        assertNull(mail.getTimeSent());*/
    }

    public void testBody() {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetBody()).thenReturn(true);        
        BodyType body = BodyType.Factory.newInstance();
        body.setStringValue("This is an email message.");
        when(mockedMessage.getBody()).thenReturn(body);

        MailboxItem mail = new ExchangeEmail(mockedMessage);/*
        assertEquals(mail.getBody(), "This is an email message.");

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetBody()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertEquals(mail.getBody(), "");*/
    }
}
