package com.softartisans.timberwolf.exchange.ldap;

import javax.naming.*;
import javax.naming.directory.*;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import com.softartisans.timberwolf.exchange.PrincipalFetchException;
import com.softartisans.timberwolf.exchange.PrincipalFetcher;
import com.sun.security.auth.callback.TextCallbackHandler;

import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

public class LdapFetcher implements PrincipalFetcher, PrivilegedAction<Iterable<String>> {
    final String _domainName;
    final String _configurationEntry;
    PrincipalFetchException fail;

    public LdapFetcher(String domainName, String configurationEntry)
    {
        if (domainName == null || domainName.length() == 0)
        {
            throw new IllegalArgumentException("domainName cannot be empty");
        }
        if (configurationEntry == null || configurationEntry.length() == 0)
        {
            throw new IllegalArgumentException("configurationEntry cannot be empty");
        }
        _domainName = domainName;
        _configurationEntry = configurationEntry;
    }

    public Iterable<String> getPrincipals() throws PrincipalFetchException
    {
        LoginContext lc = loginAsDev(_configurationEntry);
        Subject sbj = lc.getSubject();

        Iterable<String> rtn = Subject.doAs(sbj, this);
        if (fail != null)
        {
            throw fail;
        }
        return rtn;
    }

    @Override
    public Iterable<String> run() {
        Hashtable<String, String> defEnv = defaultEnvironment();
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
            e.printStackTrace();
            fail = new PrincipalFetchException(e);
        }
        return rtnList;
    }

    private Hashtable<String, String> defaultEnvironment()
    {
        Hashtable<String, String> env = new Hashtable<String, String>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        // The secrets about this PROVIDER_URL are contained within:
        // http://docs.oracle.com/javase/6/docs/technotes/guides/jndi/jndi-ldap.html
        // Check out the part about "automatic discovery of LDAP services".
        // Because that's what's happening here.
        env.put(Context.PROVIDER_URL, getProviderDiscoveryURL());
        env.put(Context.SECURITY_AUTHENTICATION, "GSSAPI");
        return env;
    }

    private static LoginContext loginAsDev(String configurationEntry) throws PrincipalFetchException
    {
        LoginContext lc = null;
        try {
            lc = new LoginContext(configurationEntry, new TextCallbackHandler());
            // Attempt authentication
            // We might want to do this in a "for" loop to give
            // user more than one chance to enter correct username/password
            lc.login();
        } catch (LoginException le) {
            System.err.println("Authentication attempt failed: " + le);
            throw new PrincipalFetchException(le);
        }

        return lc;
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

    /**
     * Given a domain name, this returns the url
     * used to automatically discover the ldap server.
     */
    public String getProviderDiscoveryURL()
    {
        StringBuilder userDNBuilder = new StringBuilder();
        userDNBuilder.append("ldap:///");
        String[] bits = _domainName.split("\\.");
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
    public Iterable<String> getSecurityMechanisms() throws PrincipalFetchException {
        try {
            Hashtable<String, String> defEnv = defaultEnvironment();
            DirContext ctx = new InitialDirContext(defEnv);

            Attributes attrs = ctx.getAttributes(getProviderDiscoveryURL(),
                                                 new String[]{"supportedSASLMechanisms"});
            List<String> rtnList = new LinkedList<String>();
            stringifyAttributes(attrs, "supportedSASLMechanisms", rtnList);
            return rtnList;

        } catch (NamingException e) {
            throw new PrincipalFetchException(e);
        }
    }
}
