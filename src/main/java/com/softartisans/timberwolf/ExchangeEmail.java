package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.types.MessageType;

import java.util.Date;

public class ExchangeEmail implements Email {
    private String body;
    private String subject;
    private Date timeSent;

    public ExchangeEmail(MessageType message) {
        if (message.isSetBody()) {
            // Body can be either HTML or plain text content.  For now we
            // don't care.
            body = message.getBody().getStringValue();
        }
        else {
            body = "";
        }

        if (message.isSetSubject()) {
            subject = message.getSubject();
        }
        else {
            subject = "";
        }

        if (message.isSetDateTimeSent()) {
            timeSent = message.getDateTimeSent().getTime();
        }
        // This isn't strictly correct, but it's going to be an okay 
        // approximation in most cases.
        else if (message.isSetDateTimeReceived()) {
            timeSent = message.getDateTimeReceived().getTime();
        }
        else {
            timeSent = null;
        }
    }

    public String getBody() {
        return body;
    }

    public String getSender() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public String[] getRecipients() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public String[] getCcRecipients() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public String[] getBccRecipients() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public String getSubject() {
        return subject;
    }

    public Date getTimeSent() {
        return timeSent;
    }

    public String[] getHeaderKeys() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public String getHeader(String header) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
