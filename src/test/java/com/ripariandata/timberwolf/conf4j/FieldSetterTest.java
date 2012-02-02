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

    @Test
    public void testSetWrongType() throws NoSuchFieldException
    {
        TypeWithOneField target = new TypeWithOneField();
        FieldSetter setter = new FieldSetter(target, TypeWithOneField.class.getDeclaredField("x"));

        try
        {
            setter.set("String");
            fail("Setting an int field to a string should have thrown an exception.");
        }
        catch (IllegalArgumentException e)
        {
            // The test is done here, but checkstyle doesn't like empty blocks.
            assertEquals(true, true);
        }
        catch (Exception e)
        {
            fail("Wrong exception thrown: " + e.getMessage());
        }

    }

    private class TypeWithAllKindsOfTypes
    {
        private byte b = 0;
        private short sh = 0;
        private int i = 0;
        private long l = 0;
        private float f = 0.0f;
        private double d = 0.0;
        private char c = '\u0000';
        private String st = null;

        public byte b()
        {
            return b;
        }

        public short sh()
        {
            return sh;
        }

        public int i()
        {
            return i;
        }

        public long l()
        {
            return l;
        }

        public float f()
        {
            return f;
        }

        public double d()
        {
            return d;
        }

        public char c()
        {
            return c;
        }

        public String st()
        {
            return st;
        }
    }

    @Test
    public void testNonOverwritingFieldSetter() throws NoSuchFieldException
    {
        TypeWithAllKindsOfTypes target = new TypeWithAllKindsOfTypes();
        Class c = TypeWithAllKindsOfTypes.class;
        FieldSetter bsetter = new NonOverwritingFieldSetter(target, c.getDeclaredField("b"));
        FieldSetter shsetter = new NonOverwritingFieldSetter(target, c.getDeclaredField("sh"));
        FieldSetter isetter = new NonOverwritingFieldSetter(target, c.getDeclaredField("i"));
        FieldSetter lsetter = new NonOverwritingFieldSetter(target, c.getDeclaredField("l"));
        FieldSetter fsetter = new NonOverwritingFieldSetter(target, c.getDeclaredField("f"));
        FieldSetter dsetter = new NonOverwritingFieldSetter(target, c.getDeclaredField("d"));
        FieldSetter stsetter = new NonOverwritingFieldSetter(target, c.getDeclaredField("st"));

        bsetter.set((byte)10);
        shsetter.set((short)100);
        isetter.set((int)1000);
        lsetter.set((long)10000);
        fsetter.set(10.5f);
        dsetter.set(100.05);
        stsetter.set("string!");

        assertEquals(10, target.b());
        assertEquals(100, target.sh());
        assertEquals(1000, target.i());
        assertEquals(10000, target.l());
        assertEquals(10.5, target.f(), .001);
        assertEquals(100.05, target.d(), .001);
        assertEquals("string!", target.st());

        bsetter.set(20);
        shsetter.set(200);
        isetter.set(2000);
        lsetter.set(20000);
        fsetter.set(20.8);
        dsetter.set(200.08);
        stsetter.set("more string?");

        assertEquals(10, target.b());
        assertEquals(100, target.sh());
        assertEquals(1000, target.i());
        assertEquals(10000, target.l());
        assertEquals(10.5, target.f(), .001);
        assertEquals(100.05, target.d(), .001);
        assertEquals("string!", target.st());
    }
}
