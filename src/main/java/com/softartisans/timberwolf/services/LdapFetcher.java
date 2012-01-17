package com.softartisans.timberwolf.services;

import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

/**
 * This class fetches a list of principals through LDAP.
 * It tries to return as many as possible.
 */
public class LdapFetcher implements PrincipalFetcher
{
    private final String domainName;

    public LdapFetcher(final String aDomainName)
    {
        if (aDomainName == null || aDomainName.length() == 0)
        {
            throw new IllegalArgumentException("domainName cannot be empty");
        }
        domainName = aDomainName;
    }

    /**
     * This will make the actual LDAP call. This will likely need to be
     * called from a privileged context (ie. from Subject.doAs()).
     *
     * @return A list of principals to get emails for.
     */
    public Iterable<String> getPrincipals() throws PrincipalFetchException
    {
        Hashtable<String, String> defEnv = defaultEnvironment();
        List<String> rtnList = new LinkedList<String>();
        try
        {
            String wantedAttribute = "userPrincipalName";
            String[] attributeFilter = {
                    wantedAttribute
            };
            SearchControls ctrl = new SearchControls();
            DirContext context = getInitialContext(defEnv);
            ctrl.setReturningAttributes(attributeFilter);
            ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
            NamingEnumeration<SearchResult> enumeration =
                    context.search("CN=Users", "(objectClass=person)", ctrl);
            while (enumeration.hasMore())
            {
                SearchResult result = enumeration.next();
                Attributes attribs = result.getAttributes();
                Attribute attrib = attribs.get(wantedAttribute);
                if (attrib != null)
                {
                    NamingEnumeration<?> values =
                        ((BasicAttribute) attrib).getAll();
                    while (values.hasMore())
                    {
                        rtnList.add(values.next().toString());
                    }
                }
            }

        }
        catch (NamingException e)
        {
            throw new PrincipalFetchException(e);
        }
        return rtnList;
    }

    /**
     * This is just pulled out so it can be overridden for testing and
     * a mock object can be returned.
     */
    DirContext getInitialContext(final Hashtable<String, String> environment) throws NamingException
    {
        return new InitialDirContext(environment);
    }

    private Hashtable<String, String> defaultEnvironment()
    {
        final Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        // The secrets about this PROVIDER_URL are contained within:
        // http://docs.oracle.com/javase/6/docs/technotes/guides/jndi/jndi-ldap.html
        // Check out the part about "automatic discovery of LDAP services".
        // Because that's what's happening here.
        env.put(Context.PROVIDER_URL, getProviderDiscoveryURL());
        env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
        return env;
    }

    /**
     * Given a domain name, this returns the url
     * used to automatically discover the ldap server.
     */
    public String getProviderDiscoveryURL()
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
