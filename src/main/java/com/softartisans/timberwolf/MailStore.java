package com.softartisans.timberwolf;

import com.cloudera.alfredo.client.AuthenticationException;

import java.io.IOException;

/**
 * MailStore represents a repository, either local or remote, of mail.
 */
public interface MailStore
{
    /** Returns some mail for the given user. */
    Iterable<MailboxItem> getMail()
            throws IOException, AuthenticationException;
}
