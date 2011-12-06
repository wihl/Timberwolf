package com.softartisans.timberwolf;

import java.util.Map;

public interface Email {
    String getBody();
    String[] getRecipients();
    String[] getCcRecipients();
    String[] getBccRecipients();
    String getSubject();
    // TODO: What other common headers do we want?

    String getHeader(String header);
}