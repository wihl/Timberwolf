package com.ripariandata.timberwolf.services;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.eq;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import junit.framework.Assert;

import org.junit.Test;
import org.mockito.Mockito;

public class LdapFetcherTest {
    final static String testDomain = "int.testDomain.com";
    final static String testConfigEntry = "Timberwolf";

    @Test
    public void getProviderDiscoveryURLTest()
    {
        LdapFetcher result = new LdapFetcher(testDomain);
        Assert.assertEquals("ldap:///dc=int,dc=testDomain,dc=com",
                            result.getProviderDiscoveryURL());
    }

    @Test
    public void getProviderDiscoveryURLEmptyTest()
    {
        LdapFetcher result = new LdapFetcher("testDomain");
        Assert.assertEquals("ldap:///dc=testDomain",
                            result.getProviderDiscoveryURL());
    }

    @Test(expected=IllegalArgumentException.class)
    public void illegalDomainNameEmptyTest()
    {
        new LdapFetcher("");
    }

    @Test(expected=IllegalArgumentException.class)
    public void illegalDomainNameNullTest()
    {
        new LdapFetcher(null);
    }

    @Test
    public void standardListTest() throws PrincipalFetchException
    {
        LdapFetcher fetcher = getTestFetcher(testDomain, testConfigEntry);
        List<String> results = asList(fetcher.getPrincipals());
        Assert.assertTrue(results.contains("first@" + testDomain));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private LdapFetcher getTestFetcher(String domainName, String configurationEntry) throws PrincipalFetchException
    {
        final DirContext context = mock(DirContext.class);
        SearchControls ctrl = new SearchControls();
        final String expectedAttribute = "userPrincipalName";
        String[][] results = { { "first" }, { "second", "second2" } };
        String[] attributeFilter = {
                expectedAttribute
        };
        ctrl.setReturningAttributes(attributeFilter);
        ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try
        {
            final TestEnumerator<SearchResult> resultEnum = new TestEnumerator<SearchResult>();
            when(context.search(eq("CN=Users"), eq("(objectClass=person)"), Mockito. any(SearchControls.class))).thenReturn(resultEnum);
            for (String[] currentResultArray : results)
            {
                SearchResult mockResult = mock(SearchResult.class);
                Attributes mockAttributes = mock(Attributes.class);
                BasicAttribute mockAttribute = mock(BasicAttribute.class);
                TestEnumerator smallBitEnum = new TestEnumerator();
                resultEnum.add(mockResult);
                when(mockResult.getAttributes()).thenReturn(mockAttributes);
                when(mockAttributes.get(expectedAttribute)).thenReturn(mockAttribute);
                when(mockAttribute.getAll()).thenReturn(smallBitEnum);
                for (String currentResult : currentResultArray)
                {
                    smallBitEnum.add(currentResult + "@" + domainName);
                }
            }
        }
        catch (NamingException e)
        {
            throw new PrincipalFetchException("An exception was thrown setting up mock objects?", e);
        }
        LdapFetcher fetcher = new LdapFetcher(testDomain)
        {
            DirContext getInitialContext(final Hashtable<String, String> environment) throws NamingException
            {
                return context;
            }
        };
        return fetcher;
    }

    private List<String> asList(Iterable<String> iter)
    {
        List<String> results = new ArrayList<String>();
        for (String str : iter)
        {
            results.add(str);
        }
        return results;
    }

    private class TestEnumerator<T> extends ArrayList<T> implements NamingEnumeration<T>
    {
        private int pointer = 0;

        @Override
        public boolean hasMoreElements() {
            return this.size() > pointer;
        }

        @Override
        public T nextElement() {
            return this.get(pointer++);
        }

        @Override
        public void close() throws NamingException {
        }

        @Override
        public boolean hasMore() throws NamingException {
            return this.size() > pointer;
        }

        @Override
        public T next() throws NamingException {
            return this.get(pointer++);
        }
    }
}
