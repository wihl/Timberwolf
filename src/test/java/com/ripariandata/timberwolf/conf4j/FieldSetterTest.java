package com.ripariandata.timberwolf.conf4j;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class FieldSetterTest
{
    private class TypeWithOneField
    {
        public int x = 0;
    }

    @Test
    public void testSetOneField() throws NoSuchFieldException
    {
        TypeWithOneField target = new TypeWithOneField();
        FieldSetter setter = new FieldSetter(target, TypeWithOneField.class.getField("x"));

        assertEquals(0, target.x);

        setter.set(10);
        assertEquals(10, target.x);

        setter.set(0);
        assertEquals(0, target.x);

        TypeWithOneField target2 = new TypeWithOneField();
        FieldSetter setter2 = new FieldSetter(target2, TypeWithOneField.class.getField("x"));

        setter2.set(-10);
        assertEquals(-10, target2.x);
        assertEquals(0, target.x);
    }

    private class TypeWithFinalField
    {
        public static final int x = 0;
    }

    @Test
    public void testIllegalFieldAccess() throws NoSuchFieldException
    {
        TypeWithFinalField target = new TypeWithFinalField();
        FieldSetter setter = new FieldSetter(target, TypeWithFinalField.class.getField("x"));

        assertEquals(0, target.x);

        try
        {
            setter.set(10);
            fail("Attempting to set value on static final field should throw exception.");
        }
        catch (IllegalAccessError e)
        {
            // Pass
        }
        catch (Exception e)
        {
            fail("Wrong exception thrown: " + e.getMessage());
        }
    }

    private class TypeWithManyFields
    {
        public int i = 0;
        public String s = "";
        public long l = 0;
    }

    @Test
    public void testSetMultipleFields() throws NoSuchFieldException
    {
        TypeWithManyFields target = new TypeWithManyFields();
        FieldSetter intSetter = new FieldSetter(target, TypeWithManyFields.class.getField("i"));
        FieldSetter stringSetter = new FieldSetter(target, TypeWithManyFields.class.getField("s"));
        FieldSetter longSetter = new FieldSetter(target, TypeWithManyFields.class.getField("l"));

        assertEquals(0, target.i);
        assertEquals("", target.s);
        assertEquals(0, target.l);

        intSetter.set(23);
        stringSetter.set("Jackdaws");
        longSetter.set(500L);

        assertEquals(23, target.i);
        assertEquals("Jackdaws", target.s);
        assertEquals(500L, target.l);

        intSetter.set(5);

        assertEquals(5, target.i);
        assertEquals("Jackdaws", target.s);
        assertEquals(500L, target.l);
    }
}
