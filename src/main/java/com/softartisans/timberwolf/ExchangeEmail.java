package com.softartisans.timberwolf;

import com.microsoft.schemas.exchange.services._2006.types.MessageType;

import java.util.Date;

public class ExchangeEmail implements Email {
    private String subject;

    public ExchangeEmail(MessageType message) {
        if (message.isSetSubject()) {
            subject = message.getSubject();
        }
        else {
            subject = "";
        }
    }

    public String getBody() {
        throw new UnsupportedOperationException("Not implemented yet.");
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
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public String[] getHeaderKeys() {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public String getHeader(String header) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
