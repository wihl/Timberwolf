/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ripariandata.timberwolf.mail.exchange;

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
