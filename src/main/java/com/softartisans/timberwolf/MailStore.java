package com.softartisans.timberwolf;

import org.joda.time.DateTime;

/**
 * MailStore represents a repository, either local or remote, of mail.
 */
public interface MailStore
{
    /** Returns some mail for the given users, starting at the given date and time. */
    Iterable<MailboxItem> getMail(Iterable<String> targetUsers, DateTime startDate);
}
