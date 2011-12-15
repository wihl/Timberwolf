package com.softartisans.timberwolf;

/**
 * Writes a series of mails to the console, for debugging purposes.
 *
 */
public final class ConsoleMailWriter implements MailWriter
{
    private static final String BETWEEN_MAIL =
            "===========================================================";

    @Override
    public void write(final Iterable<MailboxItem> mails)
    {
        for (final MailboxItem mail : mails)
        {
            System.out.println(BETWEEN_MAIL);
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
