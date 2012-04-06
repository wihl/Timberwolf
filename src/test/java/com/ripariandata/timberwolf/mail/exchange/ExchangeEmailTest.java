/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ripariandata.timberwolf.mail.exchange;

import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfRecipientsType;
import com.microsoft.schemas.exchange.services.x2006.types.BodyType;
import com.microsoft.schemas.exchange.services.x2006.types.EmailAddressType;
import com.microsoft.schemas.exchange.services.x2006.types.ItemIdType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.SingleRecipientType;
import com.ripariandata.timberwolf.mail.MailboxItem;

import java.util.Calendar;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test suite for the ExchangeEmail class.
 */
public class ExchangeEmailTest
{
    @Test
    public void testSubject()
    {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetSubject()).thenReturn(true);
        when(mockedMessage.getSubject()).thenReturn("Test Email");

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("Subject"));
        assertEquals("Test Email", mail.getHeader("Subject"));

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetSubject()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertFalse(mail.hasKey("Subject"));
        assertNull(mail.getHeader("Subject"));
    }

    @Test
    public void testTimeSent()
    {
        final int year = 2011;
        final int month = 12;
        final int day = 7;
        final int hour = 12;
        final int minute = 55;
        final int second = 30;
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetDateTimeSent()).thenReturn(true);
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hour, minute, second);
        when(mockedMessage.getDateTimeSent()).thenReturn(calendar);

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertEquals("Jan 7, 2012 12:55:30 PM", mail.getHeader("Time Sent"));

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetDateTimeSent()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertFalse(mail.hasKey("Time Sent"));
        assertNull(mail.getHeader("Time Sent"));
    }

    @Test
    public void testBody()
    {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetBody()).thenReturn(true);
        BodyType body = BodyType.Factory.newInstance();
        body.setStringValue("This is an email message.");
        when(mockedMessage.getBody()).thenReturn(body);

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("Body"));
        assertEquals("This is an email message.", mail.getHeader("Body"));

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetBody()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertFalse(mail.hasKey("Body"));
        assertNull(mail.getHeader("Body"));
    }

    @Test
    public void testItemId()
    {
        MessageType mockedMessage = mock(MessageType.class);
        ItemIdType mockedId = mock(ItemIdType.class);
        when(mockedMessage.isSetItemId()).thenReturn(true);
        when(mockedMessage.getItemId()).thenReturn(mockedId);
        when(mockedId.getId()).thenReturn("ABCD1234");

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("Item ID"));
        assertEquals("ABCD1234", mail.getHeader("Item ID"));

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetItemId()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertFalse(mail.hasKey("Item ID"));
        assertNull(mail.getHeader("Item ID"));
    }

    @Test
    public void testSender()
    {
        MessageType mockedMessage = mock(MessageType.class);
        SingleRecipientType sender = mock(SingleRecipientType.class);
        EmailAddressType address = mock(EmailAddressType.class);
        when(mockedMessage.isSetFrom()).thenReturn(true);
        when(mockedMessage.getFrom()).thenReturn(sender);
        when(sender.getMailbox()).thenReturn(address);
        when(address.isSetEmailAddress()).thenReturn(true);
        when(address.getEmailAddress()).thenReturn("seank@int.test");

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("Sender"));
        assertEquals("seank@int.test", mail.getHeader("Sender"));

        mockedMessage = mock(MessageType.class);
        sender = mock(SingleRecipientType.class);
        address = mock(EmailAddressType.class);
        when(mockedMessage.isSetFrom()).thenReturn(true);
        when(mockedMessage.getFrom()).thenReturn(sender);
        when(sender.getMailbox()).thenReturn(address);
        when(address.isSetEmailAddress()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertFalse(mail.hasKey("Sender"));
        assertNull(mail.getHeader("Sender"));

        mockedMessage = mock(MessageType.class);
        sender = mock(SingleRecipientType.class);
        address = mock(EmailAddressType.class);
        when(mockedMessage.isSetFrom()).thenReturn(false);
        when(mockedMessage.isSetSender()).thenReturn(true);
        when(mockedMessage.getSender()).thenReturn(sender);
        when(sender.getMailbox()).thenReturn(address);
        when(address.isSetEmailAddress()).thenReturn(true);
        when(address.getEmailAddress()).thenReturn("seank@int.test");

        mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("Sender"));
        assertEquals("seank@int.test", mail.getHeader("Sender"));

        mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetFrom()).thenReturn(false);

        mail = new ExchangeEmail(mockedMessage);
        assertFalse(mail.hasKey("Sender"));
        assertNull(mail.getHeader("Sender"));
    }

    private static ArrayOfRecipientsType assertRecipients()
    {
        ArrayOfRecipientsType mockedArray = mock(ArrayOfRecipientsType.class);
        EmailAddressType[] mockedAddresses = new EmailAddressType[2];
        mockedAddresses[0] = mock(EmailAddressType.class);
        mockedAddresses[1] = mock(EmailAddressType.class);

        when(mockedArray.getMailboxArray()).thenReturn(mockedAddresses);
        when(mockedAddresses[0].getEmailAddress()).thenReturn("email1@domain.com");
        when(mockedAddresses[1].getEmailAddress()).thenReturn("email2@domain.com");

        return mockedArray;
    }

    @Test
    public void testToRecipientId()
    {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetToRecipients()).thenReturn(true);
        ArrayOfRecipientsType recipients = assertRecipients();
        when(mockedMessage.getToRecipients()).thenReturn(recipients);

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("To"));
        assertEquals("email1@domain.com;email2@domain.com;", mail.getHeader("To"));
    }

    @Test
    public void testCcRecipientId()
    {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetCcRecipients()).thenReturn(true);
        ArrayOfRecipientsType recipients = assertRecipients();
        when(mockedMessage.getCcRecipients()).thenReturn(recipients);

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("Cc"));
        assertEquals("email1@domain.com;email2@domain.com;", mail.getHeader("Cc"));
    }

    @Test
    public void testBccRecipientId()
    {
        MessageType mockedMessage = mock(MessageType.class);
        when(mockedMessage.isSetBccRecipients()).thenReturn(true);
        ArrayOfRecipientsType recipients = assertRecipients();
        when(mockedMessage.getBccRecipients()).thenReturn(recipients);

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("Bcc"));
        assertEquals("email1@domain.com;email2@domain.com;", mail.getHeader("Bcc"));
    }

    @Test
    public void testMultipleProperties()
    {
        MessageType mockedMessage = mock(MessageType.class);
        BodyType mockedBody = mock(BodyType.class);
        when(mockedMessage.isSetBody()).thenReturn(true);
        when(mockedMessage.getBody()).thenReturn(mockedBody);
        when(mockedBody.getStringValue()).thenReturn("Body of an email.");
        when(mockedMessage.isSetSubject()).thenReturn(true);
        when(mockedMessage.getSubject()).thenReturn("Subject of an email.");
        when(mockedMessage.isSetToRecipients()).thenReturn(true);
        ArrayOfRecipientsType recipients = assertRecipients();
        when(mockedMessage.getToRecipients()).thenReturn(recipients);

        MailboxItem mail = new ExchangeEmail(mockedMessage);
        assertTrue(mail.hasKey("Body"));
        assertTrue(mail.hasKey("Subject"));
        assertFalse(mail.hasKey("Time Sent"));
        assertFalse(mail.hasKey("Item ID"));
        assertFalse(mail.hasKey("Sender"));
        assertTrue(mail.hasKey("To"));
        assertFalse(mail.hasKey("Cc"));
        assertFalse(mail.hasKey("Bcc"));

        String[] keys = mail.getHeaderKeys();
        final int keyLength = 3;
        assertEquals(keyLength, keys.length);
        assertEquals("Body", keys[0]);
        assertEquals("Subject", keys[1]);
        assertEquals("To", keys[2]);

        assertEquals("Body of an email.", mail.getHeader("Body"));
        assertEquals("Subject of an email.", mail.getHeader("Subject"));
        assertNull(mail.getHeader("Time Sent"));
        assertNull(mail.getHeader("Item ID"));
        assertNull(mail.getHeader("Sender"));
        assertEquals("email1@domain.com;email2@domain.com;", mail.getHeader("To"));
        assertNull(mail.getHeader("Cc"));
        assertNull(mail.getHeader("Bcc"));
    }

    @Test
    public void testPossibleHeaders()
    {
        String[] headers = ExchangeEmail.possibleHeaderKeys();
        assertEquals(8, headers.length);
        assertEquals("Body", headers[0]);
        assertEquals("Subject", headers[1]);
        assertEquals("Time Sent", headers[2]);
        assertEquals("Item ID", headers[3]);
        assertEquals("Sender", headers[4]);
        assertEquals("To", headers[5]);
        assertEquals("Cc", headers[6]);
        assertEquals("Bcc", headers[7]);
    }
}
