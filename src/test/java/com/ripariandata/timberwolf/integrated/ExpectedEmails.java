package com.ripariandata.timberwolf.integrated;

import com.ripariandata.timberwolf.exchange.RequiredEmail;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple class for holding a bunch of expected emails, and then either
 * asserting they exist, or none of them exist.
 */
public class ExpectedEmails
{
    private static final Logger LOG = LoggerFactory.getLogger(ExpectedEmails.class);
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
        if (to == null || !email.getToString().equals(to))
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
            if (cc == null || !email.getCcString().equals(cc))
            {
                return false;
            }
        }
        if (email.getBcc() != null)
        {
            String bcc = getString(result, "Bcc");
            if (bcc == null || !email.getBccString().equals(bcc))
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
            LOG.error("Missing the following emails:");
            for (RequiredEmail email : temp)
            {
                LOG.error("  " + email);
            }
            Assert.fail(
                    "Missing " + temp.size() + " required emails out of " + requiredEmails.size() + " expected emails");
        }
    }

    private void assertNoExtraResults(final List<Result> extraResults)
    {
        if (extraResults.size() > 0)
        {
            LOG.error("The following emails were in HBase but shouldn't be there:");
            for (Result result : extraResults)
            {
                LOG.error("  Result in HBase:");
                LOG.error("    Subject: {}", getString(result, "Subject"));
                LOG.error("    Body: {}", getString(result, "Body"));
                LOG.error("    To: {}", getString(result, "To"));
                LOG.error("    Sender: {}", getString(result, "Sender"));
                LOG.error("    Cc: {}", getString(result, "Cc"));
                LOG.error("    Bcc: {}", getString(result, "Bcc"));
            }
            Assert.fail("There were " + extraResults.size() + " unexpected emails in hbase in addition to the "
                        + requiredEmails.size() + " required emails.");
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

    /**
     * Removes the first RequiredEmail from the email list that matches result
     * @param emails the list of emails to check against
     * @param result the result from hbase to compare to the emails
     * @return true if a RequiredEmail matched the given result
     */
    private boolean removeResult(final List<RequiredEmail> emails, final Result result)
    {
        Iterator<RequiredEmail> p = emails.iterator();
        while (p.hasNext())
        {
            RequiredEmail email = p.next();
            if (matches(email, result))
            {
                p.remove();
                return true;
            }
        }
        return false;
    }

    public void checkHbase() throws IOException
    {
        List<RequiredEmail> temp = new LinkedList<RequiredEmail>(requiredEmails);
        HTableInterface table = hbase.getTestingTable();
        Scan scan = createScan(hbase.getFamily(), new String[]{"Subject", "Sender", "Bcc", "Cc", "To", "Body"});
        ResultScanner scanner = table.getScanner(scan);
        int countInHBase = 0;
        List<Result> extraResults = new LinkedList<Result>();
        for (Result result = scanner.next(); result != null; result = scanner.next())
        {
            countInHBase++;
            if (!removeResult(temp, result))
            {
                extraResults.add(result);
            }
        }
        assertEmpty(temp);
        assertNoExtraResults(extraResults);
    }

}
