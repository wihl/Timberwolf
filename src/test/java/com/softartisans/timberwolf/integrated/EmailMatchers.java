package com.softartisans.timberwolf.integrated;

import org.apache.hadoop.hbase.client.Result;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is basically a helper to make creating EmailMatchers easier
 */
public class EmailMatchers implements Iterable<EmailMatcher>
{
    private List<EmailMatcher> matchers;
    private String family;

    public EmailMatchers(String columnFamily)
    {
        family = columnFamily;
        matchers = new ArrayList<EmailMatcher>();
    }

    public EmailMatcher add()
    {
        EmailMatcher matcher = new EmailMatcher(family);
        matchers.add(matcher);
        return matcher;
    }

    public void match(Result result)
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

    public void assertEmpty()
    {
        if (matchers.size() > 0)
        {
            Assert.fail("Missing " + matchers.size() + " required emails");
            // TODO: actually tell you something about what's missing
        }
    }

    @Override
    public Iterator<EmailMatcher> iterator()
    {
        return matchers.iterator();
    }
}
