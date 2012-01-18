package com.softartisans.timberwolf.exchange;

import org.joda.time.DateTime;

/**
 * This class contains any configurable settings
 * that will effect the exchange service calls.
 */
public class Configuration
{
    private final int findPageSize;
    private final int getPageSize;
    private final DateTime dateTimeFrom;

    /**
     * @param findItemPageSize Must be greater than or equal to 1.
     * @param getItemPageSize
     * @param startDate The earliest date and time to look for messages.
     */
    public Configuration(final int findItemPageSize, final int getItemPageSize,
                         final DateTime startDate)
    {
        // Asking for negative or zero max items is nonsensical.
        this.findPageSize = Math.max(findItemPageSize, 1);
        this.getPageSize = Math.max(getItemPageSize, 1);
        this.dateTimeFrom = startDate;
    }

    /**
     * Creates a configuration with the startDate set to the beginning of the epoch.
     *
     * @param findItemPageSize Must be greater than or equal to 1.
     * @param getItemPageSize
     */
    public Configuration(final int findItemPageSize, final int getItemPageSize)
    {
        this(findItemPageSize, getItemPageSize, new DateTime(0));
    }

    public int getFindItemPageSize()
    {
        return findPageSize;
    }

    public int getGetItemPageSize()
    {
        return getPageSize;
    }

    public Configuration withStartDate(DateTime startDate)
    {
        return new Configuration(findPageSize, getPageSize, startDate);
    }

    public DateTime getStartDate()
    {
        return dateTimeFrom;
    }
}
