package com.softartisans.timberwolf.exchange.ldap;

import javax.naming.*;
import javax.naming.directory.*;
import java.util.Hashtable;

public class LdapFetcher {

	public static String getSecurityMechanisms(String domainName) {
        try {
            Hashtable<String, String> defEnv = defaultEnvironment(domainName);
            DirContext ctx = new InitialDirContext(defEnv);

            Attributes attrs = ctx.getAttributes(defEnv.get(Context.PROVIDER_URL),
                                                 new String[]{"supportedSASLMechanisms"});
            return stringifyAttributes(attrs, "supportedSASLMechanisms");

        } catch (Exception e) {
            e.printStackTrace();
            return "failed";
        }
    }

    private static Hashtable<String, String> defaultEnvironment(String domainName)
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        // The secrets about this PROVIDER_URL are contained within:
        // http://docs.oracle.com/javase/6/docs/technotes/guides/jndi/jndi-ldap.html
        // Check out the part about "automatic discovery of LDAP services".
        // Because that's what's happening here.
        env.put(Context.PROVIDER_URL, getProviderDiscoveryURL(domainName));
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, "developer@INT.TARTARUS.COM");
        env.put(Context.SECURITY_CREDENTIALS, "pass@word1");
        return env;
    }

    public static String getAllUsers(String domainName) {
        StringBuilder output = new StringBuilder();
        Hashtable<String, String> defEnv = defaultEnvironment(domainName);
        try {
            DirContext context = new InitialDirContext(defEnv);

            String wantedAttribute = "userPrincipalName";
            SearchControls ctrl = new SearchControls();
            String[] attributeFilter = { wantedAttribute };
            ctrl.setReturningAttributes(attributeFilter);
            ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<?> enumeration = context.search("CN=Users", "(objectClass=person)", ctrl);
            while (enumeration.hasMore()) {
                SearchResult result = (SearchResult) enumeration.next();
                Attributes attribs = result.getAttributes();
                String values = stringifyAttributes(attribs, wantedAttribute);
                if (output.length() > 0)
                {
                    output.append("\n");
                }
                output.append(values);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return output.toString();
    }

    private static String stringifyAttributes(Attributes attribs, String wantedAttribute) throws NamingException
    {
        Attribute attrib = attribs.get(wantedAttribute);
        StringBuilder output = new StringBuilder();
        if (attrib != null) {
            NamingEnumeration<?> values = ((BasicAttribute)attrib).getAll();
            while (values.hasMore()) {
                if (output.length() > 0) {
                    output.append("\n");
                }
                output.append(values.next().toString());
            }
        }
        return output.toString();
    }

    private static String getProviderDiscoveryURL(String domainName)
    {
        StringBuilder userDNBuilder = new StringBuilder();
        userDNBuilder.append("ldap:///");
        String[] bits = domainName.split("\\.");
        boolean firstBit = true;
        for (String bit : bits)
        {
            if (firstBit)
            {
                firstBit = false;
            }
            else
            {
                userDNBuilder.append(',');
            }
            userDNBuilder.append("dc=");
            userDNBuilder.append(bit);
        }
        return userDNBuilder.toString();
    }

    private static String getUserDNDirectory(String domainName)
    {
        StringBuilder userDNBuilder = new StringBuilder();
        userDNBuilder.append("CN=Users");
        if (domainName != null && domainName.length() != 0)
        {
            String[] bits = domainName.split("\\.");
            for (String bit : bits)
            {
                userDNBuilder.append(",DC=");
                userDNBuilder.append(bit);
            }
        }
        return userDNBuilder.toString();
    }

    public LdapFetcher() {}
}
