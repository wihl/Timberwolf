package com.softartisans.timberwolf.integrated;

import com.softartisans.timberwolf.exchange.RequiredEmail;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.junit.Assert;

/**
 * Simple class for holding a bunch of expected emails, and then either
 * asserting they exist, or none of them exist.
 */
public class ExpectedEmails
{
    private final List<RequiredEmail> requiredEmails;

    public ExpectedEmails()
    {
        requiredEmails = new ArrayList<RequiredEmail>();
    }

    public void require(RequiredUser user)
    {
        user.getAllEmails(requiredEmails);
    }

    private boolean matches(final RequiredEmail email, final Result result)
    {
        return false;
    }

    private void assertEmpty(final List<RequiredEmail> temp)
    {
        if (temp.size() > 0)
        {
            System.out.println("Missing the following emails:");
            for (RequiredEmail email : temp)
            {
                System.out.println("  " + email);
            }
            Assert.fail("Missing " + temp.size() + " required emails");
        }
    }

    public void checkHbaseTable(HTable table, Scan scan) throws IOException
    {
        List<RequiredEmail> temp = new LinkedList<RequiredEmail>(requiredEmails);
        ResultScanner scanner = table.getScanner(scan);
        for (Result result = scanner.next(); result != null; result = scanner.next())
        {
            Iterator<RequiredEmail> p = temp.iterator();
            while (p.hasNext())
            {
                RequiredEmail email = p.next();
                if (matches(email, result))
                {
                    p.remove();
                }
            }
        }
        assertEmpty(temp);
    }

}
