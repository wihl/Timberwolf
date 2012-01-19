package com.softartisans.timberwolf.exchange;

import com.softartisans.timberwolf.UserTimeUpdater;
import org.joda.time.DateTime;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

public class TestConfiguration
{
    @Test
    public void testConstructingConfiguration()
    {
        Configuration config = new Configuration(100, 10);
        assertEquals(100, config.getFindItemPageSize());
        assertEquals(10, config.getGetItemPageSize());

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
        Configuration config = new Configuration(10, 5);
        Configuration clone = config.withTimeUpdater(null);
        assertEquals(10, clone.getFindItemPageSize());
        assertEquals(5, clone.getGetItemPageSize());
    }

    @Test
    public void testCloningConfigurationDoesNotAlterOriginal()
    {
        Configuration config = new Configuration(10, 5);
        config.withTimeUpdater(null); // Any calls to the new updater will throw null refs.
        assertEquals(new DateTime(0), config.getLastUpdated("user"));
        config.setLastUpdated("user", new DateTime(10));
    }

    @Test
    public void testUpdatingTime()
    {
        UserTimeUpdater mockUpdater = mock(UserTimeUpdater.class);
        Configuration config = new Configuration(1, 1);
        config = config.withTimeUpdater(mockUpdater);

        config.getLastUpdated("user");
        config.getLastUpdated("otheruser");
        config.setLastUpdated("user", new DateTime(5));
        config.setLastUpdated("otheruser", new DateTime(10));

        verify(mockUpdater).lastUpdated("user");
        verify(mockUpdater).lastUpdated("otheruser");
        verify(mockUpdater).setUpdateTime("user", new DateTime(5));
        verify(mockUpdater).setUpdateTime("otheruser", new DateTime(10));
    }
}
