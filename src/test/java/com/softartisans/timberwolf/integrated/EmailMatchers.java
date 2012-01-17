package com.softartisans.timberwolf.integrated;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.hbase.client.Result;
import org.junit.Assert;

/** This is basically a helper to make creating EmailMatchers easier. */
public class EmailMatchers implements Iterable<EmailMatcher>
{
    private List<EmailMatcher> matchers;
    private String family;

    /**
     * Creates a new list of EmailMatchers.
     * @param columnFamily the column family for the table.
     */
    public EmailMatchers(final String columnFamily)
    {
        family = columnFamily;
        matchers = new ArrayList<EmailMatcher>();
    }

    /**
     * Add a new email matcher.
     * @return the added email matcher
     */
    public EmailMatcher add()
    {
        EmailMatcher matcher = new EmailMatcher(family);
        matchers.add(matcher);
        return matcher;
    }

    /**
     * Match the given result against all the email matchers.
     *
     * If an emailMatcher matches, remove it from the list
     * @param result a result from hbase that should contain a row with an email.
     */
    public void match(final Result result)
    {
        for (EmailMatcher matcher : matchers)
        {
            if (matcher.matches(result))
            {
                matchers.remove(matcher);
                return;
            }
        }
    }

    /**
     * Assert that all email matchers matched an email.
     */
    public void assertEmpty()
    {
        if (matchers.size() > 0)
        {
            System.out.println("Missing the following emails:");
            for (EmailMatcher matcher : matchers)
            {
                System.out.println("  " + matcher);
            }
            Assert.fail("Missing " + matchers.size() + " required emails");
        }
    }

    @Override
    public Iterator<EmailMatcher> iterator()
    {
        return matchers.iterator();
    }
}
