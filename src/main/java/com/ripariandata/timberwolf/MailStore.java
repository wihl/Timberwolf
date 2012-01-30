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
