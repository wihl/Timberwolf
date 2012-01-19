package com.softartisans.timberwolf.integrated;

import com.softartisans.timberwolf.exchange.RequiredEmail;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;

/**
 * Simple class for holding a bunch of expected emails, and then either
 * asserting they exist, or none of them exist.
 */
public class ExpectedEmails
{
    private final List<RequiredEmail> requiredEmails;
    private HTableResource hbase;

    public ExpectedEmails()
    {
        requiredEmails = new ArrayList<RequiredEmail>();
    }

    public void require(RequiredUser user, HTableResource hTableResource)
    {
        hbase = hTableResource;
        user.getAllEmails(requiredEmails);
    }

    private String getString(final Result result, final String columnName)
    {
        return Bytes.toString(result.getValue(Bytes.toBytes(hbase.getFamily()),
                                              Bytes.toBytes(columnName)));
    }

    private boolean matches(final RequiredEmail email, final Result result)
    {

        String subject = getString(result, "Subject");
        if (subject == null || !email.getSubject().equals(subject))
        {
            return false;
        }
        String body = getString(result, "Body");
        if (body == null || !email.getBody().equals(body))
        {
            return false;
        }
        String to = getString(result, "To");
        if (to == null || !email.getTo().equals(to))
        {
            return false;
        }
        if (email.getFrom() != null)
        {
            String from = getString(result, "Sender");
            if (from == null || !email.getFrom().equals(from))
            {
                return false;
            }
        }
        if (email.getCc() != null)
        {
            String cc = getString(result, "Cc");
            if (cc == null || !email.getCc().equals(cc))
            {
                return false;
            }
        }
        if (email.getBcc() != null)
        {
            String bcc = getString(result, "Bcc");
            if (bcc == null || !email.getBcc().equals(bcc))
            {
                return false;
            }
        }

        return true;
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

    private Scan createScan(String columnFamily, String[] headers)
    {
        Scan scan = new Scan();
        for( String header : headers)
        {
            scan.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(header));
        }
        return scan;
    }

    public void checkHbase() throws IOException
    {
        List<RequiredEmail> temp = new LinkedList<RequiredEmail>(requiredEmails);
        HTableInterface table = hbase.getTestingTable();
        Scan scan = createScan(hbase.getFamily(), new String[]{"Subject", "Sender", "Bcc", "Cc", "To", "Body"});
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
