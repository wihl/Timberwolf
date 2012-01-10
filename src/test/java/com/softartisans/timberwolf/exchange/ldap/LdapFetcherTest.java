package com.softartisans.timberwolf.exchange.ldap;

import java.util.ArrayList;
import java.util.List;

import junit.framework.*;

import com.softartisans.timberwolf.exchange.PrincipalFetchException;

import org.junit.Test;

public class LdapFetcherTest {
    final static String domainTartarus = "int.tartarus.com";
    final static String testConfigEntry = "Timberwolf";

    @Test
    public void getProviderDiscoveryURLTest()
    {
        LdapFetcher result = new LdapFetcher(domainTartarus, testConfigEntry);
        Assert.assertEquals("ldap:///dc=int,dc=tartarus,dc=com",
                            result.getProviderDiscoveryURL());
    }
    
    @Test
    public void getProviderDiscoveryURLEmptyTest()
    {
        LdapFetcher result = new LdapFetcher("tartarus", testConfigEntry);
        Assert.assertEquals("ldap:///dc=tartarus",
                            result.getProviderDiscoveryURL());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void illegalDomainNameEmptyTest()
    {
        new LdapFetcher("", "someConfiguration");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void illegalDomainNameNullTest()
    {
        new LdapFetcher(null, "someConfiguration");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void illegalConfigurationEntryEmptyTest()
    {
        new LdapFetcher("tartarus", "");
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void illegalConfigurationEntryNullTest()
    {
        new LdapFetcher("tartarus", null);
    }
    
    // I'm not sure yet how to test the bulk of the code.
    /*
    public void firstTest() throws PrincipalFetchException
    {
        List<String> results = asList(new LdapFetcher(domainTartarus, testConfigEntry).getPrincipals());
        Assert.assertTrue(results.contains("BKerr@int.tartarus.com"));
    }

    @Test
    public void securityTest() throws PrincipalFetchException
    {
        printIterable(new LdapFetcher(domainTartarus, testConfigEntry).getSecurityMechanisms());
    }
    */

    private List<String> asList(Iterable<String> iter)
    {
        List<String> results = new ArrayList<String>();
        for (String str : iter)
        {
            results.add(str);
        }
        return results;
    }
}
