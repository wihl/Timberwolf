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

    /**
     * If possible, sets the field represented by this setter to the given value.
     *
     * @throws IllegalAccessException If the field cannot be assigned to, e.g.,
     *                                it is declared <tt>static</tt> and <tt>final</tt>.
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
