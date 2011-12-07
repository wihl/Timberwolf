package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.types.MessageType;
import com.microsoft.schemas.exchange.services._2006.types.EmailAddressType;

import java.util.HashMap;
import java.util.Map;

/**
 * ExchangeEmail represents an email message from an Exchange server.
 */
public class ExchangeEmail implements MailboxItem {
    private static final String BODY_KEY = "Body";
    private static final String SUBJECT_KEY = "Subject";
    private static final String TIME_SENT_KEY = "Time Sent";
    private static final String ID_KEY = "Item ID";
    private static final String SENDER_KEY = "Sender";

    /** The headers that this email exports. */
    private Map<String, String> headers;

    public ExchangeEmail(final MessageType message) {
        headers = new HashMap<String, String>();

        if (message.isSetBody()) {
            headers.put(BODY_KEY, message.getBody().getStringValue());
        }

        if (message.isSetSubject()) {
            headers.put(SUBJECT_KEY, message.getSubject());
        }

        if (message.isSetDateTimeSent()) {
            String time = message.getDateTimeSent().getTime().toString();
            headers.put(TIME_SENT_KEY, time);
        }

        if (message.isSetItemId()) {
            headers.put(ID_KEY, message.getItemId().getId());
        }

        if (message.isSetFrom()) {
            EmailAddressType address = message.getFrom().getMailbox();
            if (address.isSetEmailAddress()) {
                headers.put(SENDER_KEY, address.getEmailAddress());
            }
        }
    }

    public final String[] getHeaderKeys() {
        return headers.keySet().toArray(new String[0]);
    }

    public final boolean hasKey(final String key) {
        return headers.containsKey(key);
    }

    public final String getHeader(final String key) {
        return headers.get(key);
    }
}
