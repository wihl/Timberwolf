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
 * FieldSetter wraps around a single field in a single object, so that later
 * we can set the value of the that field.
 */
public class FieldSetter
{
    private Object bean;
    private Field field;

    public FieldSetter(final Object o, final Field f)
    {
        bean = o;
        field = f;
    }

    protected Object bean()
    {
        return bean;
    }

    protected Field field()
    {
        return field();
    }

    /**
     * If possible, sets the field represented by this setter to the given value.
     *
     * @throws IllegalAccessError If the field cannot be assigned to, e.g., it
     *                            is declared <tt>static</tt> and <tt>final</tt>.
     * @throws IllegalArgumentException If the target field's type is not compatible
     *                                  with <tt>value</tt>'s type.
     */
    public void set(final Object value)
    {
        try
        {
            field.set(bean, value);
        }
        catch (IllegalAccessException iae)
        {
            field.setAccessible(true);
            try
            {
                field.set(bean, value);
            }
            catch (IllegalAccessException e)
            {
                throw new IllegalAccessError(e.getMessage());
            }
        }
    }
}
