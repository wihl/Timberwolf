package com.softartisans.timberwolf;

import static org.junit.Assert.*;
import org.junit.Test;

public class ExchangeMailStoreTest
{
    @Test
    public void testGetSomeMail()
    {
        ExchangeMailStore store = new ExchangeMailStore("abenjamin", "pass@word1", "https://int.tartarus.com/ews/exchange.asmx");
        store.getMail("abenjamin");
    }
}
