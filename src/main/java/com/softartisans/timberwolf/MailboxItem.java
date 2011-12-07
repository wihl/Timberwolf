package com.softartisans.timberwolf;

/**
 * MailboxItem represents a generic item from a mailbox.
 *
 * MailboxItems export a number of headers identified by keys. Exactly which
 * keys are exported, and what pieces of data they correspond to, is
 * defined by implementers of MailboxItem.
 */
public interface MailboxItem {
    /** Returns all the keys that this item exports. */
    String[] getHeaderKeys();

    /** Returns true if this item exports the given key, false otherwise. */
    boolean hasKey(String key);

    /**
     * Returns the data associated with the given key, or null if that key
     * isn't exported by this item.
     */
    String getHeader(String key);
}
