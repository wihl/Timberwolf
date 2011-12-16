package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services.x2006.types.ArrayOfRecipientsType;
import com.microsoft.schemas.exchange.services.x2006.types.EmailAddressType;
import com.microsoft.schemas.exchange.services.x2006.types.MessageType;
import com.microsoft.schemas.exchange.services.x2006.types.SingleRecipientType;

import java.text.DateFormat;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ExchangeEmail represents an email message from an Exchange server.
 *
 * Keys that this MailboxItem <em>may</em> export:
 * <ul>
 * <li>"Body": The body text of the email (in either HTML or plain text).
 * <li>"Subject": The subject line of the email.
 * <li>"Time Sent": The time the email was sent, formatted with the MEDIUM date
 * and time formatting from the US locale.
 * <li>"Item ID": The unique ID assigned to this item by Exchange.
 * <li>"Sender": The email address of the user who sent the email.
 * <li>"To": A semicolon-delimited list of recipients.
 * <li>"Cc": A semicolon-delimited list of cc-recipients.
 * <li>"Bcc": A semicolon-delimited list of bcc-recipients.
 * </ul>
 */
public class ExchangeEmail implements MailboxItem
{
    private static final String BODY_KEY = "Body";
    private static final String SUBJECT_KEY = "Subject";
    private static final String TIME_SENT_KEY = "Time Sent";
    private static final String ID_KEY = "Item ID";
    private static final String SENDER_KEY = "Sender";
    private static final String TORECIPIENT_KEY = "To";
    private static final String CCRECIPIENT_KEY = "Cc";
    private static final String BCCRECIPIENT_KEY = "Bcc";
    private static final char EMAIL_DELIMITER = ';';

    /** The headers that this email exports. */
    private final Map<String, String> headers;

    public ExchangeEmail(final MessageType message)
    {
        headers = new HashMap<String, String>();

        if (message.isSetBody())
        {
            headers.put(BODY_KEY, message.getBody().getStringValue());
        }

        if (message.isSetSubject())
        {
            headers.put(SUBJECT_KEY, message.getSubject());
        }

        if (message.isSetDateTimeSent())
        {
            Date time = message.getDateTimeSent().getTime();
            // I'd really like an equivalent of C#'s InvariantCulture, but it
            // looks like this is the closest I can get in Java.
            DateFormat format = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.MEDIUM, Locale.US);
            headers.put(TIME_SENT_KEY, format.format(time));
        }

        if (message.isSetItemId())
        {
            headers.put(ID_KEY, message.getItemId().getId());
        }

        // There isn't any documentation on the difference between Sender and
        // From.  I'm preferring From here purely based on the example response
        // given at:
        // http://msdn.microsoft.com/en-us/library/aa566013(v=EXCHG.140).aspx.
        SingleRecipientType sender = null;
        if (message.isSetFrom())
        {
            sender = message.getFrom();
        }
        else if (message.isSetSender())
        {
            sender = message.getSender();
        }
        if (sender != null)
        {
            EmailAddressType address = sender.getMailbox();
            if (address.isSetEmailAddress())
            {
                headers.put(SENDER_KEY, address.getEmailAddress());
            }
        }

        if (message.isSetToRecipients())
        {
            ArrayOfRecipientsType toRecipients = message.getToRecipients();
            headers.put(TORECIPIENT_KEY, getRecipientString(toRecipients));
        }

        if (message.isSetCcRecipients())
        {
            ArrayOfRecipientsType ccRecipients = message.getCcRecipients();
            headers.put(CCRECIPIENT_KEY, getRecipientString(ccRecipients));
        }

        if (message.isSetBccRecipients())
        {
            ArrayOfRecipientsType bccRecipients = message.getBccRecipients();
            headers.put(BCCRECIPIENT_KEY, getRecipientString(bccRecipients));
        }
    }

    private static String getRecipientString(
            final ArrayOfRecipientsType recipients)
    {
        StringBuilder emailList = new StringBuilder();
        for (EmailAddressType address : recipients.getMailboxArray())
        {
            emailList.append(address.getEmailAddress());
            emailList.append(EMAIL_DELIMITER);
        }
        return emailList.toString();
    }

    public final String[] getHeaderKeys()
    {
        return headers.keySet().toArray(new String[0]);
    }

    public final boolean hasKey(final String key)
    {
        return headers.containsKey(key);
    }

    public final String getHeader(final String key)
    {
        return headers.get(key);
    }
}
