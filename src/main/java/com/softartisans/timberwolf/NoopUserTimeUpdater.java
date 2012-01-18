package com.softartisans.timberwolf;

import org.joda.time.DateTime;

/**
 * An implementation of UserTimeUpdater that does nothing and always returns the
 * start of the epoch.  This is for when we're running against the console, or other
 * times we don't actually want persistence.
 */
public class NoopUserTimeUpdater implements UserTimeUpdater
{
    @Override
    public DateTime lastUpdated(String user)
    {
        return new DateTime(0);
    }

    @Override
    public void setUpdateTime(String user, DateTime dateTime)
    {
    }
}
