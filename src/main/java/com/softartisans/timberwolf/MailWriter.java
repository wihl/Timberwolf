package com.softartisans.timberwolf;

import java.util.Iterator;

public interface MailWriter
{
    void write(Iterator<MailboxItem> mails);
}
