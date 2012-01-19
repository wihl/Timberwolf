package com.softartisans.timberwolf.exchange;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import org.joda.time.DateTime;
import org.junit.Test;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
}
