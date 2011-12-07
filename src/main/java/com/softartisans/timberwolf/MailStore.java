package com.softartisans.timberwolf;

import java.util.Iterator;

public interface MailStore
{
    Iterator<MailboxItem> getMail(String user);
}
