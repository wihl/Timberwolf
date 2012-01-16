package com.softartisans.timberwolf.services;

import com.sun.security.auth.callback.TextCallbackHandler;

import java.security.PrivilegedAction;
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
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * This class fetches a list of principals through LDAP.
 * It tries to return as many as possible.
 */
public class LdapFetcher implements PrincipalFetcher, PrivilegedAction<Iterable<String>>
{
    private final String domainName;
    private final String configurationEntry;
    private PrincipalFetchException fail;

    public LdapFetcher(final String aDomainName, final String aConfigurationEntry)
    {
        if (aDomainName == null || aDomainName.length() == 0)
        {
            throw new IllegalArgumentException("domainName cannot be empty");
        }
        if (aConfigurationEntry == null || aConfigurationEntry.length() == 0)
        {
            throw new IllegalArgumentException("configurationEntry cannot be empty");
        }
        domainName = aDomainName;
        configurationEntry = aConfigurationEntry;
    }

    public Iterable<String> getPrincipals() throws PrincipalFetchException
    {
        LoginContext lc = login(configurationEntry);
        Subject sbj = lc.getSubject();

        Iterable<String> rtn = Subject.doAs(sbj, this);
        if (fail != null)
        {
            throw fail;
        }
        return rtn;
    }

    /**
     * This will make the actual LDAP call. This is the run function of the
     * privileged action, so it assumes that all authentication has already
     * been taken care of through SASL. This should not be called directly,
     * it's called by Subject.doAs(). You should call getPrincipals, which
     * takes care of logging in. If you're already logged in? Then this
     * class needs to be touched up to remove all the login stuff.
     *
     * @return A list of principals to get emails for.
     */
    @Override
    public Iterable<String> run()
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
            e.printStackTrace();
            fail = new PrincipalFetchException(e);
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
     * This will likely be removed down the line once SASL authentication
     * is global to the entire process.
     */
    private static LoginContext login(final String configurationEntry)
        throws PrincipalFetchException
    {
        LoginContext lc = null;
        try
        {
            lc = new LoginContext(configurationEntry, new TextCallbackHandler());
            // Attempt authentication
            // We might want to do this in a "for" loop to give
            // user more than one chance to enter correct username/password
            lc.login();
        }
        catch (LoginException le)
        {
            System.err.println("Authentication attempt failed: " + le);
            throw new PrincipalFetchException(le);
        }

        return lc;
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

    /** Queries the domain's LDAP server for supported authentication methods.
     *
     * I'm leaving this hear for the time being because it's quite likely
     * we may face LDAP servers that don't support GSSAPI.
     * @return An iterable listing all supported authentication methods.
     * @throws PrincipalFetchException
     */
    public Iterable<String> getSecurityMechanisms() throws PrincipalFetchException
    {
        try
        {
            Hashtable<String, String> defEnv = defaultEnvironment();
            DirContext ctx = new InitialDirContext(defEnv);

            Attributes attrs = ctx.getAttributes(getProviderDiscoveryURL(),
                                                 new String[]{"supportedSASLMechanisms"});
            List<String> rtnList = new LinkedList<String>();
            Attribute attrib = attrs.get("supportedSASLMechanisms");
            if (attrib != null)
            {
                NamingEnumeration<?> values =
                    ((BasicAttribute) attrib).getAll();
                while (values.hasMore())
                {
                    rtnList.add(values.next().toString());
                }
            }
            return rtnList;

        }
        catch (NamingException e)
        {
            throw new PrincipalFetchException(e);
        }
    }
}
