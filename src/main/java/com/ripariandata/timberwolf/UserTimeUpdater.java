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

import org.joda.time.DateTime;

/**
 * Allows determining when a user was last setUpdateTime and setting that the user has been setUpdateTime.
 */
public interface UserTimeUpdater
{
    /**
     * Determines when the given user was last updated.
     * @return When the user was last updated.
     */
    DateTime lastUpdated(String user);

    /**
     * Sets that the user has been setUpdateTime at this datetime.
     * @param user The user who has been updated.
     * @param dateTime The datetime of the update.
     */
    void setUpdateTime(String user, DateTime dateTime);
}
