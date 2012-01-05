package com.softartisans.timberwolf.exchange.ldap;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.Hashtable;

public class LdapFetcher {

    public static String getAllUsers(String domainName) {
        StringBuilder output = new StringBuilder();
        
        String userDNDirectory = getUserDNDirectory(domainName);

        try {
            String url = "ldap://devexch01.int.tartarus.com";
            Hashtable<String, String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, url);
            env.put(Context.SECURITY_AUTHENTICATION, "simple");
            env.put(Context.SECURITY_PRINCIPAL, "developer@INT.TARTARUS.COM");
            env.put(Context.SECURITY_CREDENTIALS, "pass@word1");
            DirContext context = new InitialDirContext(env);

            String wantedAttribute = "userPrincipalName";
            SearchControls ctrl = new SearchControls();
            String[] attributeFilter = { wantedAttribute };
            ctrl.setReturningAttributes(attributeFilter);
            ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<?> enumeration = context.search(userDNDirectory, "(objectClass=person)", ctrl);
            while (enumeration.hasMore()) {
                SearchResult result = (SearchResult) enumeration.next();
                Attributes attribs = result.getAttributes();
                Attribute attrib = attribs.get(wantedAttribute);
                if (attrib != null) {
                  NamingEnumeration<?> values = ((BasicAttribute)attrib).getAll();
                  while (values.hasMore()) {
                    if (output.length() > 0) {
                      output.append("\n");
                    }
                    output.append(values.next().toString());
                  }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }
    
    private static String getUserDNDirectory(String domainName)
    {
    	String[] bits = domainName.split("\\.");
    	StringBuilder userDNBuilder = new StringBuilder();
    	userDNBuilder.append("CN=Users");
    	for (String bit : bits)
    	{
    		userDNBuilder.append(",DC=");
    		userDNBuilder.append(bit);
    	}
    	return userDNBuilder.toString();
    }

    public LdapFetcher() {}
}