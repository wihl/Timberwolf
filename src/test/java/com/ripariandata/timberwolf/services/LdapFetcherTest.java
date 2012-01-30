/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ripariandata.timberwolf.services;

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

import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test suite for the LdapFetcher.
 */
public class LdapFetcherTest
{
    static final String TEST_DOMAIN = "int.testDomain.com";
    static final String TEST_CONFIG_ENTRY = "Timberwolf";

    @Test
    public void getProviderDiscoveryURLTest()
    {
        LdapFetcher result = new LdapFetcher(TEST_DOMAIN);
        Assert.assertEquals("ldap:///dc=int,dc=testDomain,dc=com",
                            result.getProviderDiscoveryURL());
    }

    @Test
    public void getProviderDiscoveryURLEmptyTest()
    {
        LdapFetcher result = new LdapFetcher("testDomain");
        Assert.assertEquals("ldap:///dc=testDomain", result.getProviderDiscoveryURL());
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalDomainNameEmptyTest()
    {
        new LdapFetcher("");
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalDomainNameNullTest()
    {
        new LdapFetcher(null);
    }

    @Test
    public void standardListTest() throws PrincipalFetchException
    {
        LdapFetcher fetcher = getTestFetcher(TEST_DOMAIN, TEST_CONFIG_ENTRY);
        List<String> results = asList(fetcher.getPrincipals());
        Assert.assertTrue(results.contains("first@" + TEST_DOMAIN));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private LdapFetcher getTestFetcher(final String domainName, final String configurationEntry)
            throws PrincipalFetchException
    {
        final DirContext context = mock(DirContext.class);
        SearchControls ctrl = new SearchControls();
        final String expectedAttribute = "userPrincipalName";
        String[][] results = {{"first"}, {"second", "second2"}};
        String[] attributeFilter = {
                expectedAttribute
        };
        ctrl.setReturningAttributes(attributeFilter);
        ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);
        try
        {
            final TestEnumerator<SearchResult> resultEnum = new TestEnumerator<SearchResult>();
            when(context.search(eq("CN=Users"), eq("(objectClass=person)"), Mockito.any(SearchControls.class)))
                    .thenReturn(resultEnum);
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
        LdapFetcher fetcher = new LdapFetcher(TEST_DOMAIN)
        {
            DirContext getInitialContext(final Hashtable<String, String> environment) throws NamingException
            {
                return context;
            }
        };
        return fetcher;
    }

    private List<String> asList(final Iterable<String> iter)
    {
        List<String> results = new ArrayList<String>();
        for (String str : iter)
        {
            results.add(str);
        }
        return results;
    }

    /**
     * Testing Enumerator class.
     * @param <T> The type of the TestEnumerator collection.
     */
    private class TestEnumerator<T> extends ArrayList<T> implements NamingEnumeration<T>
    {
        private int pointer = 0;

        @Override
        public boolean hasMoreElements()
        {
            return this.size() > pointer;
        }

        @Override
        public T nextElement()
        {
            return this.get(pointer++);
        }

        @Override
        public void close() throws NamingException
        {
        }

        @Override
        public boolean hasMore() throws NamingException
        {
            return this.size() > pointer;
        }

        @Override
        public T next() throws NamingException
        {
            return this.get(pointer++);
        }
    }
}
