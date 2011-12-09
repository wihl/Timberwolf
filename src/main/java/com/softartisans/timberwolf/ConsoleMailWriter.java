package com.softartisans.timberwolf;

import java.util.Iterator;
import java.lang.System;

/**
 * Writes a series of mails to the console, for debugging purposes
 *
 */
public class ConsoleMailWriter implements MailWriter
{
    @Override
    public void write(Iterable<MailboxItem> mails)
    {
        for (MailboxItem mail : mails)
        {
            System.out.println("===========================================================");
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
