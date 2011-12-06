package com.softartisans.timberwolf;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ExchangeEmailTest extends TestCase {
    public ExchangeEmailTest(String testName) {
        super(testName);
    }

    public static Test suite() {
        return new TestSuite(ExchangeEmailTest.class);
    }

    public void testDummy() {
        assertTrue(true);
    }
}
