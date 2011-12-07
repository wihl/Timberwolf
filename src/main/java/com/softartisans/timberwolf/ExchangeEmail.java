package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.types.MessageType;

import java.util.HashMap;
import java.util.Map;

public class ExchangeEmail implements MailboxItem {
    private Map<String, String> headers;

    public ExchangeEmail(MessageType message) {
        headers = new HashMap<String, String>();

        if (message.isSetBody()) {
            headers.put("Body", message.getBody().getStringValue());
        }

        if (message.isSetSubject()) {
            headers.put("Subject", message.getSubject());
        }

        if (message.isSetDateTimeSent()) {
            headers.put("Time Sent", message.getDateTimeSent().getTime().toString());
        }
    }

    public String[] getHeaderKeys() {
        return headers.keySet().toArray(new String[0]);
    }

    public boolean hasKey(String key) {
        return headers.containsKey(key);
    }

    public String getHeader(String key) {
        return headers.get(key);
    }
}
