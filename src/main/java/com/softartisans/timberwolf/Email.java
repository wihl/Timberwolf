package com.softartisans.timberwolf;

import java.util.Map;

public interface Email {
    String getBody();
    String getSender();
    String[] getRecipients();
    String[] getCcRecipients();
    String[] getBccRecipients();
    String getSubject();
    DateTime getTimeSent();
    // TODO: What other common headers do we want?

    String getHeader(String header);
}