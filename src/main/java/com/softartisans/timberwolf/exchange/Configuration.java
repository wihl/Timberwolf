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
        if (findItemPageSize < 1)
        {
            throw new IllegalArgumentException("findItem page size must be greater than or equal to 1.");
        }
        if (getItemPageSize < 1)
        {
            throw new IllegalArgumentException("getItem page size must be greater than or equal to 1");
        }
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
