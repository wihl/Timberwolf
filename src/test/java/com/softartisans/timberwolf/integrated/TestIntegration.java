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

        Below is the required structure; identation denotes the heirarchy.
        Some of the folders have a required count, that's
        denoted by having the count in parentheses after the folder name.
        The contents of the emails are defined in the actual code, by adding
        EmailMatchers to requiredEmails.
        Note that if you put html tags in the body (such as changing formatting),
        that is considered text:

          korganizer@*
            Inbox
              child of Inbox
              Inbox jr
            Drafts
            Sent Items
            Deleted Items
              Deleted Folder
            Topper
              Middler
                Middler Jr
                  Middler II (0)
                    Middler III
                      Middler IV (0)
                Ms child
            Page FindItems (29)
            Page GetItems (11)

         */
        String keyHeader = "Item ID";

        EmailMatchers requiredEmails = new EmailMatchers(hbase.getFamily());

        // Inbox
        requiredEmails.add()
                      .Sender("tsender")
                      .Subject("Leave it be")
                      .BodyContains("love your inbox clean");
        // child of Inbox
        requiredEmails.add()
                      .Subject("To the child of inbox")
                      .BodyContains("child of Inbox");
        // Inbox Jr
        requiredEmails.add()
                      .Bcc("korganizer")
                      .BodyContains("Inbox Jr")
                      .BodyContains("is getting lonely");
        requiredEmails.add().To("korganizer").BodyContains("InboxJr");
        // Drafts
        requiredEmails.add().To("tsender").Subject("A draft");
        // Sent Items
        requiredEmails.add()
                      .Sender("korganizer")
                      .To("abenjamin")
                      .Subject("A message to someone else");
        // Deleted Items
        requiredEmails.add().Subject("Whoops").BodyContains("this is trash");
        // Deleted Folder
        requiredEmails.add().BodyContains("Deleted Folder");
        // Topper
        requiredEmails.add()
                      .To("bkerr")
                      .Cc("korganizer")
                      .Subject("Hey hey Bobbie, throw it in the Topper");
        // Middler
        requiredEmails.add().BodyContains("away this should go into middler, placed neatly.");
        requiredEmails.add().Subject("Another middler");
        requiredEmails.add().Subject("Yet another in the middler");
        // Middler Jr
        requiredEmails.add().Subject("organize away in MJ").BodyContains("Middler Jr");
        // Middler II (0)
        // Middler III
        requiredEmails.add().Subject("Forward to Middler III");
        // Middler IV (0)
        // Ms child
        requiredEmails.add().BodyContains("Ms child").BodyContains("nicer than MJ");
        requiredEmails.add()
                      .Subject("Super nesting")
                      .BodyContains("Ms child")
                      .BodyContains("wants to be in the loop too.");
        // Page FindItems (29)
        for (int i=1; i<30; i++)
        {
            requiredEmails.add()
                          .Subject("Page FindItems" + i)
                          .BodyContains("Page FindItems")
                          .BodyContains("#" + i);
        }
        // Page GetItems (11)
        for (int i=1; i<12; i++)
        {
            requiredEmails.add()
                          .Subject("Page GetItems" + i)
                          .BodyContains("Page GetItems")
                          .BodyContains("#" + i);
        }


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
            for (Result result = scanner.next(); result != null; result = scanner.next())
            {
                requiredEmails.match(result);
            }
            requiredEmails.assertEmpty();
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
