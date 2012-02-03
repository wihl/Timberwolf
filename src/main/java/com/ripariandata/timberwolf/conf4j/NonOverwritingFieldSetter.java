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
package com.ripariandata.timberwolf.conf4j;

import java.lang.reflect.Field;

/**
 * NonOverwritingFieldSetter wraps around a single field in a single object, so
 * so that later we can set the value of that field.  Calling <tt>set</tt> on a
 * field whose value is not the default value for the field's type will do nothing.
 * <p>
 * The default value is 0 for numeric types, false for booleans, '\u0000' for chars,
 * and null for everything else.
 */
class NonOverwritingFieldSetter extends FieldSetter
{
    private Object defaultValue;

    public NonOverwritingFieldSetter(final Object o, final Field f)
    {
        super(o, f);
        defaultValue = get(o, f);
    }

    private static Object get(Object bean, Field field)
    {
        Object val;
        try
        {
            val = field.get(bean);
        }
        catch (IllegalAccessException e)
        {
            field.setAccessible(true);
            try
            {
                val = field.get(bean);
            }
            catch (IllegalAccessException iae)
            {
                throw new IllegalAccessError(iae.getMessage());
            }
        }
        return val;
    }

    /**
     * Sets the field represented by this setter to the given value as long as
     * the field currently contains the default value for its type.
     *
     * @throws IllegalAccessError If the field cannot be assigned to.
     * @throws IllegalArgumentException If the target field's type is not compatible
     *                                  with <tt>value</tt>'s type.
     */
    @Override
    public void set(final Object value)
    {
        Object val = get(bean(), field());

        if ((defaultValue == null && val == null)
            || (defaultValue != null && val != null && defaultValue.equals(val)))
        {
            super.set(value);
        }
    }
}
