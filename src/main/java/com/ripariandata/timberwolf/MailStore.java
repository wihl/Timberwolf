package com.ripariandata.timberwolf;

/**
 * MailStore represents a repository, either local or remote, of mail.
 */
public interface MailStore
{
    /** Returns some mail for the given users. */
    Iterable<MailboxItem> getMail(Iterable<String> targetUsers);
}
