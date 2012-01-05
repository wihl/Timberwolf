package com.softartisans.timberwolf.exchange.ldap;

import org.junit.Test;

public class LdapFetcherTest {
	final static String domainTartarus = "int.tartarus.com";
	@Test
	public void firstTest()
	{
		String rtn = LdapFetcher.getAllUsers(domainTartarus);
		System.out.print(rtn);
	}

	@Test
	public void securityTest()
	{
		String rtn = LdapFetcher.getSecurityMechanisms(domainTartarus);
		System.out.print(rtn);
	}
}
