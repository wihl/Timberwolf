package com.softartisans.timberwolf.exchange.ldap;

import javax.naming.*;
import javax.naming.directory.*;

import com.softartisans.timberwolf.exchange.PrincipalFetchException;
import com.softartisans.timberwolf.exchange.PrincipalFetcher;

import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class LdapFetcher implements PrincipalFetcher {
    final String _domainName;

    public LdapFetcher(String domainName)
    {
        _domainName = domainName;
    }

    public static Iterable<String> getSecurityMechanisms(String domainName) throws PrincipalFetchException {
        try {
            Hashtable<String, String> defEnv = defaultEnvironment(domainName);
            DirContext ctx = new InitialDirContext(defEnv);

            Attributes attrs = ctx.getAttributes("ldap://devexch01.int.tartarus.com",
                                                 new String[]{"supportedSASLMechanisms"});
            List<String> rtnList = new LinkedList<String>();
            stringifyAttributes(attrs, "supportedSASLMechanisms", rtnList);
            return rtnList;

        } catch (NamingException e) {
            throw new PrincipalFetchException(e);
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
        //env.put(Context.PROVIDER_URL, "ldap://devexch01.int.tartarus.com");
        env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
        //env.put(Context.SECURITY_AUTHENTICATION, "simple");
        //env.put(Context.SECURITY_PRINCIPAL, "developer");
        //env.put(Context.SECURITY_CREDENTIALS, "pass@word1");
        return env;
    }

    public Iterable<String> getPrincipals() throws PrincipalFetchException
    {
        Hashtable<String, String> defEnv = defaultEnvironment(_domainName);
        List<String> rtnList = new LinkedList<String>();
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
                stringifyAttributes(attribs, wantedAttribute, rtnList);
            }

        } catch (NamingException e) {
            throw new PrincipalFetchException(e);
        }
        return rtnList;
    }

    private static void stringifyAttributes(Attributes attribs, String wantedAttribute, Collection<String> collector) throws NamingException
    {
        Attribute attrib = attribs.get(wantedAttribute);
        if (attrib != null) {
            NamingEnumeration<?> values = ((BasicAttribute)attrib).getAll();
            while (values.hasMore()) {
                collector.add(values.next().toString());
            }
        }
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
}
