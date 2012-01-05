package com.softartisans.timberwolf.exchange.ldap;

import org.junit.Test;

public class LdapFetcherTest {
	
	@Test
	public void firstTest()
	{
		String rtn = LdapFetcher.getAllUsers("int.tartarus.com");
		System.out.print(rtn);
	}
}
