package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.types.MessageType;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Sean Kermes <seank@softartisans.com>
 *
 * ExchangeEmail represents an email message from an Exchange server.
 */
public class ExchangeEmail implements MailboxItem {
    /** The headers that this email exports. */
    private Map<String, String> headers;

    public ExchangeEmail(final MessageType message) {
        headers = new HashMap<String, String>();

        if (message.isSetBody()) {
            headers.put("Body", message.getBody().getStringValue());
        }

        if (message.isSetSubject()) {
            headers.put("Subject", message.getSubject());
        }

        if (message.isSetDateTimeSent()) {
            String time = message.getDateTimeSent().getTime().toString();
            headers.put("Time Sent", time);
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
