package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.UserTimeUpdater;
import org.joda.time.DateTime;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/** Tests the Configuration class. */
public class TestConfiguration
{
    @Test
    public void testConstructingConfiguration()
    {
        final int findItemSize = 100;
        final int getItemSize = 10;
        Configuration config = new Configuration(findItemSize, getItemSize);
        assertEquals(findItemSize, config.getFindItemPageSize());
        assertEquals(getItemSize, config.getGetItemPageSize());

        config = new Configuration(0, 0);
        assertEquals(1, config.getFindItemPageSize());
        assertEquals(1, config.getGetItemPageSize());
    }

    @Test
    public void testDefaultTimeUpdater()
    {
        Configuration config = new Configuration(1, 1);
        assertEquals(new DateTime(0), config.getLastUpdated("user"));
        config.setLastUpdated("user", new DateTime(1));
        assertEquals(new DateTime(0), config.getLastUpdated("user"));
    }

    @Test
    public void testCloningConfigurationWithNewTimeUpdater()
    {
        final int findItemSize = 10;
        final int getItemSize = 5;
        Configuration config = new Configuration(findItemSize, getItemSize);
        Configuration clone = config.withTimeUpdater(null);
        assertEquals(findItemSize, clone.getFindItemPageSize());
        assertEquals(getItemSize, clone.getGetItemPageSize());
    }

    @Test
    public void testCloningConfigurationDoesNotAlterOriginal()
    {
        Configuration config = new Configuration(0, 0);
        config.withTimeUpdater(null); // Any calls to the new updater will throw null refs.
        assertEquals(new DateTime(0), config.getLastUpdated("user"));
        config.setLastUpdated("user", new DateTime(1));
    }

    @Test
    public void testUpdatingTime()
    {
        UserTimeUpdater mockUpdater = mock(UserTimeUpdater.class);
        Configuration config = new Configuration(1, 1);
        config = config.withTimeUpdater(mockUpdater);

        final int userTime = 5;
        final int otherUserTime = 10;

        config.getLastUpdated("user");
        config.getLastUpdated("otheruser");
        config.setLastUpdated("user", new DateTime(userTime));
        config.setLastUpdated("otheruser", new DateTime(otherUserTime));

        verify(mockUpdater).lastUpdated("user");
        verify(mockUpdater).lastUpdated("otheruser");
        verify(mockUpdater).setUpdateTime("user", new DateTime(userTime));
        verify(mockUpdater).setUpdateTime("otheruser", new DateTime(otherUserTime));
    }
}
