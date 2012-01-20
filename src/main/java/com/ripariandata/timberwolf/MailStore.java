package com.ripariandata.timberwolf;

/**
 * MailStore represents a repository, either local or remote, of mail.
 */
public interface MailStore
{
    /**
     * Returns some mail for the given users, only getting the mail for each user since the
     * time returned by timeUpdater.lastUpdated.
     */
    Iterable<MailboxItem> getMail(Iterable<String> targetUsers, UserTimeUpdater timeUpdater);
}
