package com.softartisans.timberwolf.exchange.ldap;

import com.softartisans.timberwolf.exchange.PrincipalFetchException;

import org.junit.Test;

public class LdapFetcherTest {
	final static String domainTartarus = "int.tartarus.com";

	@Test
	public void firstTest()
	{
		try
        {
            printIterable(new LdapFetcher(domainTartarus).getPrincipals());
        }
        catch (PrincipalFetchException e)
        {
            e.printStackTrace();
        }
	}

	@Test
	public void securityTest()
	{
	    try
	    {
	        printIterable(LdapFetcher.getSecurityMechanisms(domainTartarus));
	    }
	    catch (PrincipalFetchException e)
	    {
	        e.printStackTrace();
	    }
	}

	private void printIterable(Iterable<String> iter)
	{
	    for (String str : iter)
	    {
	        System.out.println(str);
	    }
	}
}
