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
    public NonOverwritingFieldSetter(final Object o, final Field f)
    {
        super(o, f);
    }

    private static Object getDefault(final Class c)
    {
        if (c.equals(byte.class))
        {
            return new Byte((byte) 0);
        }
        else if (c.equals(short.class))
        {
            return new Short((short) 0);
        }
        else if (c.equals(int.class))
        {
            return new Integer(0);
        }
        else if (c.equals(long.class))
        {
            return new Long(0);
        }
        else if (c.equals(float.class))
        {
            return new Float(0.0);
        }
        else if (c.equals(double.class))
        {
            return new Double(0.0);
        }
        else if (c.equals(boolean.class))
        {
            return false;
        }
        else if (c.equals(char.class))
        {
            return '\u0000';
        }

        return null;
    }

    /**
     * Sets the field represented by this setter to the given value as long as
     * the field currently contains the default value for its type.
     *
     * @throws IllegalAccessError If the field cannot read from, or if the field
     *                            cannot be assigned to.
     * @throws IllegalArgumentException If the target field's type is not compatible
     *                                  with <tt>value</tt>'s type.
     */
    @Override
    public void set(final Object value)
    {
        Object o;
        try
        {
            o = field().get(bean());
        }
        catch (IllegalAccessException iae)
        {
            field.setAccessible(true);
            try
            {
                o = field().get(bean());
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalAccessError(e.getMessage());
            }
        }

        Object defaultValue = getDefault(field().getType());
        if ((defaultValue == null && o == null)
            || (defaultValue != null && o != null && defaultValue.equals(o)))
        {
            super.set(value);
        }
    }
}
