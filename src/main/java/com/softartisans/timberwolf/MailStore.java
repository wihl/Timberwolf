package com.softartisans.timberwolf;

import java.util.Iterator;

/**
 * MailStore represents a repository, either local or remote, of mail.
 */
public interface MailStore {
    /** Returns some mail for the given user. */
    Iterator<MailboxItem> getMail(String user);
}
