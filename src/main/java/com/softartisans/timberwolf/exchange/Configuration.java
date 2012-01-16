package com.softartisans.timberwolf.exchange;

/**
 * This class contains any configurable settings
 * that will effect the exchange service calls.
 */
public class Configuration
{
    private final int findPageSize;
    private final int getPageSize;

    public Configuration(final int findItemPageSize,
                         final int getItemPageSize)
    {
        this.findPageSize = findItemPageSize;
        this.getPageSize = getItemPageSize;
    }

    public int getFindItemPageSize()
    {
        return findPageSize;
    }

    public int getGetItemPageSize()
    {
        return getPageSize;
    }
}
