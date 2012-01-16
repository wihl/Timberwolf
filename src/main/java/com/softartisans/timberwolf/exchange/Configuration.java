package com.softartisans.timberwolf.exchange;

/**
 * This class contains any configurable settings
 * that will effect the exchange service calls.
 */
public class Configuration
{
    private final int findPageSize;
    private final int getPageSize;

    /**
     *
     * @param findItemPageSize Must be greater than or equal to 1.
     * @param getItemPageSize
     */
    public Configuration(final int findItemPageSize,
                         final int getItemPageSize)
    {
        this.findPageSize = Math.max(findItemPageSize, 1);
        this.getPageSize = Math.max(getItemPageSize, 1);
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
