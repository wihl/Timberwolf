package com.softartisans.timberwolf;

import java.util.Map;

public interface Email {
    String getBody();
    String[] getRecipients();
    String[] getCcRecipients();
    String[] getBccRecipients();
    String getSubject();
    // TODO: What other common headers do we want?

    // TODO: Preference?
    Map<String, String> getHeaders();
    // OR
    Map<HEADER_TYPE, String> getHeaders();
    // OR
    String getHeader(String header);
}