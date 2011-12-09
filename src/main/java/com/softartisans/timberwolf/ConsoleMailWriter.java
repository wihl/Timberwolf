package com.softartisans.timberwolf;

import java.lang.System;

/**
 * Writes a series of mails to the console, for debugging purposes.
 *
 */
public final class ConsoleMailWriter implements MailWriter
{
    private final static String betweenMails = 
            "===========================================================";
    
    @Override
    public void write(Iterable<MailboxItem> mails)
    {
        for (final MailboxItem mail : mails)
        {
            System.out.println(betweenMails);
            for (String key : mail.getHeaderKeys())
            {
                System.out.print(key);
                System.out.print(": ");

                String value = mail.getHeader(key);
                System.out.println(value);
            }
        }
    }
}
