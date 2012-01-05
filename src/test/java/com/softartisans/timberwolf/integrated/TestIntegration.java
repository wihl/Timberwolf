package com.softartisans.timberwolf.integrated;

import com.softartisans.timberwolf.MailStore;
import com.softartisans.timberwolf.MailWriter;
import com.softartisans.timberwolf.MailboxItem;
import com.softartisans.timberwolf.exchange.ExchangeMailStore;
import com.softartisans.timberwolf.hbase.HBaseMailWriter;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.client.ResultScanner;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Overall integration testing for timberwolf.
 */
public class TestIntegration
{
    private static final String EXCHANGE_URI_PROPERTY_NAME = "ExchangeURI";

    @Rule
    public IntegrationTestProperties properties = new IntegrationTestProperties(EXCHANGE_URI_PROPERTY_NAME);

    @Rule
    public HTableResource hbase = new HTableResource();

    private Get createGet(String row, String columnFamily, String[] headers)
    {
            Get get = new Get(Bytes.toBytes(row));
            for( String header : headers)
            {
                get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(header));
            }
        return get;
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

    @Test
    public void testIntegrationNoCLI()
    {
        /*
        This test tests getting emails from an exchange server, and the breadth
        of what that entails, and then putting it all in Exchange. It does so by
        assuming a certain structure of emails on the exchange server, and then
        asserting that those emails are in the hbase table afterwards. If you're
        recreating this structure, avoid putting the text that we check in
        multiple emails, except the sender, that's fine.

        Below is the required structure; emails are prefixed with "E:" accounts
        are listed as emails, and everything else is a folder; identation
        denotes the heirarchy. Some of the folders have a required count, that's
        denoted by having the count in parentheses after the folder name.
        * and ? are used like in bash to denote what we ignore.
        Note that if you put html tags in there (such as formatting), that is
        considered text:

          korganizer@*
            Inbox
              E: Sender: tsender@*
                 Subject: Leave it be
                 Body: *love your inbox clean*
              child of Inbox
                E: Subject: To the child of inbox
                   Body: *child of Inbox*
              Inbox jr
                E: Bcc: korganizer@*
                   Body: Inbox Jr*is getting lonely
                E: To: korganizer@*
                   Body: *InboxJr*
            Drafts
              E: To: tsender@*
                 Subject: A draft
            Sent Items
              E: Sender: korganizer@*
                 To: abenjamin@*
                 Subject: A message to someone else
            Deleted Items
              E: Subject: Whoops
                 Body: *this is trash*
              Deleted Folder
                E: Body: *Deleted Folder*
            Topper
              E: To: bkerr@*
                 Cc: korganizer@*
                 Subject: Hey hey Bobbie, throw it in the Topper
              Middler
                E: Body: *away this should go into middler, placed neatly.*
                E: Subject: Another middler
                E: Subject Yet another in the middler
                Middler Jr
                  E: Subject: organize away to MJ
                     Body: *Middler Jr*
                  Middler II (0)
                    Middler III
                      E: Subject: Forward to Middler III
                      Middler IV (0)
                Ms child
                  E: Body: *Ms child*nicer than MJ
                  E: Subject: Super nesting
                     Body: *Ms child*wants to be in the loop too.
            Page FindItems (29)
              E: Subject: Page FindItems1
                 Body: *Page FindItems*#1
              E: Subject: Page FindItems2
                 Body: *Page FindItems*#2
              E: Subject: Page FindItems3
                 Body: *Page FindItems*#3
              E: Subject: Page FindItems4
                 Body: *Page FindItems*#4
              E: Subject: Page FindItems5
                 Body: *Page FindItems*#5
              E: Subject: Page FindItems6
                 Body: *Page FindItems*#6
              E: Subject: Page FindItems7
                 Body: *Page FindItems*#7
              E: Subject: Page FindItems8
                 Body: *Page FindItems*#8
              E: Subject: Page FindItems9
                 Body: *Page FindItems*#9
              E: Subject: Page FindItems10
                 Body: *Page FindItems*#10
              E: Subject: Page FindItems11
                 Body: *Page FindItems*#11
              E: Subject: Page FindItems12
                 Body: *Page FindItems*#12
              E: Subject: Page FindItems13
                 Body: *Page FindItems*#13
              E: Subject: Page FindItems14
                 Body: *Page FindItems*#14
              E: Subject: Page FindItems15
                 Body: *Page FindItems*#15
              E: Subject: Page FindItems16
                 Body: *Page FindItems*#16
              E: Subject: Page FindItems17
                 Body: *Page FindItems*#17
              E: Subject: Page FindItems18
                 Body: *Page FindItems*#18
              E: Subject: Page FindItems19
                 Body: *Page FindItems*#19
              E: Subject: Page FindItems20
                 Body: *Page FindItems*#20
              E: Subject: Page FindItems21
                 Body: *Page FindItems*#21
              E: Subject: Page FindItems22
                 Body: *Page FindItems*#22
              E: Subject: Page FindItems23
                 Body: *Page FindItems*#23
              E: Subject: Page FindItems24
                 Body: *Page FindItems*#24
              E: Subject: Page FindItems25
                 Body: *Page FindItems*#25
              E: Subject: Page FindItems26
                 Body: *Page FindItems*#26
              E: Subject: Page FindItems27
                 Body: *Page FindItems*#27
              E: Subject: Page FindItems28
                 Body: *Page FindItems*#28
              E: Subject: Page FindItems29
                 Body: *Page FindItems*#29
            Page GetItems (11)
              E: Subject: Page GetItems1
                 Body: *Page GetItems*#1
              E: Subject: Page GetItems2
                 Body: *Page GetItems*#2
              E: Subject: Page GetItems3
                 Body: *Page GetItems*#3
              E: Subject: Page GetItems4
                 Body: *Page GetItems*#4
              E: Subject: Page GetItems5
                 Body: *Page GetItems*#5
              E: Subject: Page GetItems6
                 Body: *Page GetItems*#6
              E: Subject: Page GetItems7
                 Body: *Page GetItems*#7
              E: Subject: Page GetItems8
                 Body: *Page GetItems*#8
              E: Subject: Page GetItems9
                 Body: *Page GetItems*#9
              E: Subject: Page GetItems10
                 Body: *Page GetItems*#10
              E: Subject: Page GetItems11
                 Body: *Page GetItems*#11

         */
        String keyHeader = "Item ID";

        List<EmailMatcher> requiredEmails = new ArrayList<EmailMatcher>();
        /*
         E: Sender: tsender@*
                 Subject: Leave it be
                 Body: *love your inbox clean*
         */
        requiredEmails.add(new EmailMatcher(hbase.getFamily()).Sender("tsender")
                                                              .Subject("Leave it be")
                                                              .BodyContains("love your inbox clean"));

        String exchangeURL = IntegrationTestProperties.getProperty(EXCHANGE_URI_PROPERTY_NAME);

        MailStore mailStore = new ExchangeMailStore(exchangeURL);
        MailWriter mailWriter = HBaseMailWriter.create(hbase.getTable(), keyHeader, hbase.getFamily());

        Iterable<MailboxItem> mailboxItems = mailStore.getMail();
        Assert.assertTrue(mailboxItems.iterator().hasNext());
        mailWriter.write(mailboxItems);

        // Now prove that everything is in HBase.

        try
        {
            HTableInterface hTable = hbase.getTestingTable();
            Scan scan = createScan(hbase.getFamily(), new String[] {"Subject", "Sender","Bcc","Cc","To","Body"});
            ResultScanner scanner = hTable.getScanner(scan);
            OUTER: for (Result result = scanner.next(); result != null; result = scanner.next())
            {
                for (EmailMatcher matcher : requiredEmails)
                {
                    if (matcher.matches(result))
                    {
                        requiredEmails.remove(matcher);
                        continue OUTER;
                    }
                }
            }
            if (requiredEmails.size() > 0)
            {
                Assert.fail("Missing " + requiredEmails.size() + " required emails");
                // TODO: actually tell you something about what's missing
            }
            Iterable<MailboxItem> mails = mailStore.getMail();

            for (MailboxItem mail : mails)
            {
                Get get = createGet(mail.getHeader(keyHeader),hbase.getFamily(),mail.getHeaderKeys());
                System.err.println(StringUtils.join(mail.getHeaderKeys()));
                Result result = hTable.get(get);
                for(String header : mail.getHeaderKeys())
                {
                    String tableValue = Bytes.toString(result.getValue(Bytes.toBytes(hbase.getFamily()),
                            Bytes.toBytes(header)));
                    Assert.assertEquals(mail.getHeader(header),tableValue);
                }
            }
        }
        catch (Exception e)
        {
            Assert.fail("Error when attempting to compare.");
        }
    }
}
