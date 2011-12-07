package com.softartisans.timberwolf;

public interface MailboxItem
{
    String[] getHeaderKeys();
    boolean hasKey(String key);
    String getHeader(String key);
}
