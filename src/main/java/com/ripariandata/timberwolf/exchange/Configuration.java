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
package com.ripariandata.timberwolf.exchange;

import com.ripariandata.timberwolf.NoopUserTimeUpdater;
import com.ripariandata.timberwolf.UserTimeUpdater;

import org.joda.time.DateTime;

/**
 * This class contains any configurable settings
 * that will effect the exchange service calls.
 */
public class Configuration
{
    private final int findPageSize;
    private final int getPageSize;
    private final UserTimeUpdater timeUpdater;

    /**
     * @param findItemPageSize Must be greater than or equal to 1.
     * @param getItemPageSize
     * @param userTimeUpdater The UserTimeUpdater that defines the time range for each user.
     */
    public Configuration(final int findItemPageSize, final int getItemPageSize,
                         final UserTimeUpdater userTimeUpdater)
    {
        // Asking for negative or zero max items is nonsensical.
        this.findPageSize = Math.max(findItemPageSize, 1);
        this.getPageSize = Math.max(getItemPageSize, 1);
        this.timeUpdater = userTimeUpdater;
    }

    /**
     * Creates a configuration with the startDate set to the beginning of the epoch.
     *
     * @param findItemPageSize Must be greater than or equal to 1.
     * @param getItemPageSize
     */
    public Configuration(final int findItemPageSize, final int getItemPageSize)
    {
        this(findItemPageSize, getItemPageSize, new NoopUserTimeUpdater());
    }

    public int getFindItemPageSize()
    {
        return findPageSize;
    }

    public int getGetItemPageSize()
    {
        return getPageSize;
    }

    public Configuration withTimeUpdater(final UserTimeUpdater userTimeUpdater)
    {
        return new Configuration(findPageSize, getPageSize, userTimeUpdater);
    }

    public DateTime getLastUpdated(final String user)
    {
        return timeUpdater.lastUpdated(user);
    }

    public void setLastUpdated(final String user, final DateTime time)
    {
        timeUpdater.setUpdateTime(user, time);
    }
}
