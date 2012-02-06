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

import com.ripariandata.timberwolf.InMemoryUserFolderSyncStateStorage;
import com.ripariandata.timberwolf.UserFolderSyncStateStorage;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/** Tests the Configuration class. */
public class TestConfiguration
{
    @Test
    public void testConstructingConfiguration()
    {
        final int idPageSize = 100;
        final int getItemSize = 10;
        Configuration config = new Configuration(idPageSize, getItemSize);
        assertEquals(idPageSize, config.getIdPageSize());
        assertEquals(getItemSize, config.getItemPageSize());

        config = new Configuration(0, 0);
        assertEquals(1, config.getIdPageSize());
        assertEquals(1, config.getItemPageSize());
    }

    @Test
    public void testDefaultSyncState()
    {
        Configuration config = new Configuration(2, 1);
        Assert.assertTrue(config.getSyncStateStorage() instanceof InMemoryUserFolderSyncStateStorage);
    }

    @Test
    public void testConfigurationWithNewSyncStateStorage()
    {
        final int idPageSize = 13;
        final int getItemSize = 3;
        Configuration config = new Configuration(idPageSize, getItemSize);
        Assert.assertTrue(config.getSyncStateStorage() instanceof InMemoryUserFolderSyncStateStorage);
        InMemoryUserFolderSyncStateStorage newSyncStorage = new InMemoryUserFolderSyncStateStorage();
        final UserFolderSyncStateStorage oldSyncStateStorage = config.getSyncStateStorage();
        assertNotSame(oldSyncStateStorage, newSyncStorage);
        Configuration newConfig = config.withSyncStateStorage(newSyncStorage);
        assertEquals(idPageSize, newConfig.getIdPageSize());
        assertEquals(getItemSize, newConfig.getItemPageSize());
        assertSame(newSyncStorage, newConfig.getSyncStateStorage());
        assertSame(oldSyncStateStorage, config.getSyncStateStorage());
    }
}
