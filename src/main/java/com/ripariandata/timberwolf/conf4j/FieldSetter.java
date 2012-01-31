package com.ripariandata.timberwolf.conf4j;

import java.lang.reflect.Field;

public class FieldSetter
{
    private Object bean;
    private Field field;

    public FieldSetter(Object o, Field f)
    {
        bean = o;
        field = f;
    }

    public void set(Object value)
    {
        try
        {
            field.set(bean, value);
        }
        catch (IllegalAccessException _)
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
