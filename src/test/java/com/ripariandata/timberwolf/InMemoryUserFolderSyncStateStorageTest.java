package com.ripariandata.timberwolf;

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
