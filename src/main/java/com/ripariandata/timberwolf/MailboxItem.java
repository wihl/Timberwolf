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
public interface MailboxItem
{
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
