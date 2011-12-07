package com.softartisans.timberwolf;

import java.util.Iterator;

/**
 * @author Sean Kermes <seank@softartisans.com>
 *
 * MailWriter is an object that can take some MailboxItems and write them into
 * some repository.
 */
public interface MailWriter {
    /** Writes the given MailboxItems into the repository. */
    void write(Iterator<MailboxItem> mails);
}
