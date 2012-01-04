package com.softartisans.timberwolf;

/**
 * MailStore represents a repository, either local or remote, of mail.
 */
public interface MailStore
{
    /** Returns some mail for the given user. */
    Iterable<MailboxItem> getMail();
}
