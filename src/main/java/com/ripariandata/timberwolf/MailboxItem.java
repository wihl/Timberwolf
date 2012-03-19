/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ripariandata.timberwolf;

/**
 * MailboxItem represents a generic item from a mailbox.
 *
 * MailboxItems export a number of headers identified by keys. Exactly which
 * keys are exported, and what pieces of data they correspond to, is
 * defined by implementers of MailboxItem.
 */
public abstract class MailboxItem
{
    protected static final String BODY_KEY = "Body";
    protected static final String SUBJECT_KEY = "Subject";
    protected static final String TIME_SENT_KEY = "Time Sent";
    protected static final String ID_KEY = "Item ID";
    protected static final String SENDER_KEY = "Sender";
    protected static final String TORECIPIENT_KEY = "To";
    protected static final String CCRECIPIENT_KEY = "Cc";
    protected static final String BCCRECIPIENT_KEY = "Bcc";
    protected static final char EMAIL_DELIMITER = ';';

    /**
     * Returns all the keys that implementations of MailboxItem <em>might</em>
     * export.  In valid implementations, getHeaderKeys() will be a subset
     * of possibleHeaderKeys().
     */
    public static String[] possibleHeaderKeys()
    {
        return new String[] {BODY_KEY, SUBJECT_KEY, TIME_SENT_KEY, ID_KEY, SENDER_KEY, TORECIPIENT_KEY,
                              CCRECIPIENT_KEY, BCCRECIPIENT_KEY };
    }

    /** Returns all the keys that this item exports. */
    public abstract String[] getHeaderKeys();

    /** Returns true if this item exports the given key, false otherwise. */
    public abstract boolean hasKey(String key);

    /**
     * Returns the data associated with the given key, or null if that key
     * isn't exported by this item.
     */
    public abstract String getHeader(String key);
}
