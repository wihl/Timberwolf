package com.ripariandata.timberwolf.conf4j;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/** Test for the FieldSetter used in our config file handling. */
public class FieldSetterTest
{
    /** Class for testing setting one field. */
    private class TypeWithOneField
    {
        private int x = 0;

        public int x()
        {
            return x;
        }
    }

    @Test
    public void testSetOneField() throws NoSuchFieldException
    {
        TypeWithOneField target = new TypeWithOneField();
        FieldSetter setter = new FieldSetter(target, TypeWithOneField.class.getDeclaredField("x"));

        assertEquals(0, target.x());

        setter.set(10);
        assertEquals(10, target.x());

        setter.set(0);
        assertEquals(0, target.x());

        TypeWithOneField target2 = new TypeWithOneField();
        FieldSetter setter2 = new FieldSetter(target2, TypeWithOneField.class.getDeclaredField("x"));

        setter2.set(-10);
        assertEquals(-10, target2.x());
        assertEquals(0, target.x());
    }

    /** Class for testing attempting to set an un-settable field. */
    private final class TypeWithFinalField
    {
        private static final int X = 0;

        public int x()
        {
            return X;
        }
    }

    @Test
    public void testIllegalFieldAccess() throws NoSuchFieldException
    {
        TypeWithFinalField target = new TypeWithFinalField();
        FieldSetter setter = new FieldSetter(target, TypeWithFinalField.class.getDeclaredField("X"));

        assertEquals(0, target.x());

        try
        {
            setter.set(10);
            fail("Attempting to set value on static final field should throw exception.");
        }
        catch (IllegalAccessError e)
        {
            // We don't care about anything once we get here, just that the
            // right exception type was thrown.  But Checkstyle complains about
            // blocks with no statements.
            assertTrue(true);
        }
        catch (Exception e)
        {
            fail("Wrong exception thrown: " + e.getMessage());
        }
    }

    /** Class for testing setting multiple fields in one class. */
    private class TypeWithManyFields
    {
        private int i = 0;
        private String s = "";
        private long l = 0;

        public int i()
        {
            return i;
        }
        public String s()
        {
            return s;
        }
        public long l()
        {
            return l;
        }
    }

    @Test
    public void testSetMultipleFields() throws NoSuchFieldException
    {
        TypeWithManyFields target = new TypeWithManyFields();
        FieldSetter intSetter = new FieldSetter(target, TypeWithManyFields.class.getDeclaredField("i"));
        FieldSetter stringSetter = new FieldSetter(target, TypeWithManyFields.class.getDeclaredField("s"));
        FieldSetter longSetter = new FieldSetter(target, TypeWithManyFields.class.getDeclaredField("l"));

        assertEquals(0, target.i());
        assertEquals("", target.s());
        assertEquals(0, target.l());

        intSetter.set(23);
        stringSetter.set("Jackdaws");
        longSetter.set(500L);

        assertEquals(23, target.i());
        assertEquals("Jackdaws", target.s());
        assertEquals(500L, target.l());

        intSetter.set(5);

        assertEquals(5, target.i());
        assertEquals("Jackdaws", target.s());
        assertEquals(500L, target.l());
    }
}
