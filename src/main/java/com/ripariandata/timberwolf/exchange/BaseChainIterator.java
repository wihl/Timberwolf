package com.ripariandata.timberwolf.exchange;

import java.util.Iterator;

/**
 * Base iterator class for wrapping a series of iterators into one iterator.
 *
 * @param <T> the type being iterated over. This will be the most inner link
 * in the chain
 */
public abstract class BaseChainIterator<T> implements Iterator<T>
{
    private Iterator<T> currentIterator;

    public BaseChainIterator()
    {
    }

    /**
     * Returns the next iterator for which hasNext returns true, or null.
     * @return the next iterator for which hasNext is true, or null if no
     * such iterator exists.
     */
    private Iterator<T> nextViableIterator()
    {
        while (true)
        {
            Iterator<T> iterator = createIterator();
            if (iterator == null)
            {
                return null;
            }
            if (iterator.hasNext())
            {
                return iterator;
            }
        }
    }

    /**
     * Creates the next iterator.
     * @return the next iterator that can be created,
     * or null if there are no more iterators to be created.
     */
    protected abstract Iterator<T> createIterator();

    /**
     * Returns true if there are more elements in any of the iterators.
     * @return true if there are more items to be returned by next().
     */
    @Override
    public boolean hasNext()
    {
        // always return false if currentIterator is null
        if (currentIterator != null && currentIterator.hasNext())
        {
            return true;
        }
        currentIterator = nextViableIterator();
        return currentIterator != null;
    }

    @Override
    public T next()
    {
        if (hasNext())
        {
            return currentIterator.next();
        }
        return null;
    }

    @Override
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
