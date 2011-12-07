package com.softartisans.timberwolf;

import java.util.Iterator;

/**
 * @author Sean Kermes <seank@softartisans.com>
 *
 * MailStore represents a repository, either local or remote, of mail.
 */
public interface MailStore {
    /** Returns some mail for the given user. */
    Iterator<MailboxItem> getMail(String user);
}
