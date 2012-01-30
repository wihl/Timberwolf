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
 * An implementation of UserTimeUpdater that does nothing and always returns the
 * start of the epoch.  This is for when we're running against the console, or other
 * times we don't actually want persistence.
 */
public class NoopUserTimeUpdater implements UserTimeUpdater
{
    @Override
    public DateTime lastUpdated(final String user)
    {
        return new DateTime(0);
    }

    @Override
    public void setUpdateTime(final String user, final DateTime dateTime)
    {
    }
}
