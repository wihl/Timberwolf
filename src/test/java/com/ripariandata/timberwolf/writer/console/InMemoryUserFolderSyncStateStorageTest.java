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
package com.ripariandata.timberwolf.writer.console;

import com.ripariandata.timberwolf.writer.UserFolderSyncStateStorage;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Tests for the InMemoryUserFolderSyncStateStorage.
 * This can also serve as a template for other
 * UserFolderSyncStateStorage tests.
 */
public class InMemoryUserFolderSyncStateStorageTest
{

    private UserFolderSyncStateStorage storage;

    @Before
    public void setUp() throws Exception
    {
        storage = new InMemoryUserFolderSyncStateStorage();
    }

    @Test
    public void testNoUser() throws Exception
    {
        assertNull(storage.getLastSyncState("notUser", "noFolder"));
    }

    @Test
    public void testNoFolder()
    {
        storage.setSyncState("user", "folder", "state");
        assertNull(storage.getLastSyncState("user", "noFolder"));
    }

    @Test
    public void testIsSet() throws Exception
    {
        storage.setSyncState("user", "folder", "state");
        assertEquals("state", storage.getLastSyncState("user", "folder"));
    }

    @Test
    public void testUpdate()
    {
        storage.setSyncState("user", "folder", "state");
        assertEquals("state", storage.getLastSyncState("user", "folder"));
        storage.setSyncState("user", "folder", "state2");
        assertEquals("state2", storage.getLastSyncState("user", "folder"));
    }

    @Test
    public void testUnset()
    {
        storage.setSyncState("user", "folder", "state");
        assertEquals("state", storage.getLastSyncState("user", "folder"));
        storage.setSyncState("user", "folder", null);
        assertNull(storage.getLastSyncState("user", "folder"));
    }

    @Test
    public void testMultipleFolders()
    {
        storage.setSyncState("user", "folder", "state");
        storage.setSyncState("user", "folder2", "state2");
        assertEquals("state", storage.getLastSyncState("user", "folder"));
        assertEquals("state2", storage.getLastSyncState("user", "folder2"));
    }

    @Test
    public void testMultipleUsers()
    {
        storage.setSyncState("user", "folder", "state");
        storage.setSyncState("user2", "folder", "state2");
        assertEquals("state", storage.getLastSyncState("user", "folder"));
        assertEquals("state2", storage.getLastSyncState("user2", "folder"));
    }
}
