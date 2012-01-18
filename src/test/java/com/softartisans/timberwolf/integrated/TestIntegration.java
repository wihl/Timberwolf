package com.softartisans.timberwolf.integrated;

import com.softartisans.timberwolf.Auth;
import com.softartisans.timberwolf.MailStore;
import com.softartisans.timberwolf.MailWriter;
import com.softartisans.timberwolf.MailboxItem;
import com.softartisans.timberwolf.exchange.ExchangeMailStore;
import com.softartisans.timberwolf.hbase.HBaseMailWriter;
import com.softartisans.timberwolf.services.LdapFetcher;
import com.softartisans.timberwolf.services.PrincipalFetchException;
import java.security.PrivilegedAction;
import javax.security.auth.login.LoginException;
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
 * Overall integration testing, from exchange server to hbase.
 */
public class TestIntegration
{
    private static final String EXCHANGE_URI_PROPERTY_NAME = "ExchangeURI";

    private static final String LDAP_DOMAIN_PROPERTY_NAME = "LdapDomain";
    private static final String LDAP_CONFIG_ENTRY_PROPERTY_NAME = "LdapConfigEntry";

    @Rule
    public IntegrationTestProperties properties = new IntegrationTestProperties(EXCHANGE_URI_PROPERTY_NAME,
                                                                                LDAP_DOMAIN_PROPERTY_NAME,
                                                                                LDAP_CONFIG_ENTRY_PROPERTY_NAME);

    @Rule
    public HTableResource hbase = new HTableResource();

    private Get createGet(final String row, final String columnFamily, final String[] headers)
    {
            Get get = new Get(Bytes.toBytes(row));
            for (String header : headers)
            {
                get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(header));
            }
        return get;
    }

    private Scan createScan(final String columnFamily, final String[] headers)
    {
        Scan scan = new Scan();
        for (String header : headers)
        {
            scan.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(header));
        }
        return scan;
    }

    @Test
    public void testIntegrationNoCLI() throws PrincipalFetchException, LoginException
    {
        /*
        This test tests getting emails from an exchange server, and the breadth
        of what that entails, and then putting it all in Exchange. It does so by
        assuming a certain structure of emails on the exchange server, and then
        asserting that those emails are in the hbase table afterwards. If you're
        recreating this structure, avoid putting the text that we check in
        multiple emails, except the sender, that's fine.

        There are 5 users involved, some of which are in a security group set
        up for impersonation (more on this below):
          jclouseau - the user with impersonation rights for the group
          korganizer - in the impersonation group, has a lot of folders and
                       emails
          aloner - another user in the impersonation group with just a few
                   emails
          marcher - a third user in the impersonation group, also has just a
                    few emails
          tsender - A helper user that is not in the impersonation group and
                    does all of the sending

        Before creating the security group for impersonation, create the user
        mailboxes in exchanges.

        In order to create a security group set up for impersonation:
        One will have to log into our Exchange test server. From there,
        open up the Exchange Management Console and select Recipient
        Configuration and then Distribution Group. Right click inside the pane
        and select New Distribution Group. On the first page, select new group.
        On the second, one must specify the group type as security. Don't
        specify an organization unit, and the names and aliases are arbitrary
        but make them all the same. We will refer to this unique name as the
        SecurityGroupName. On the last page simply press the New button and
        the group should be created. It's not instantaneous, but should appear
        within the list fairly quickly.

        The users will then have to be added to the security group.
        Right-click on the security group name and select Properties.
        Under the members tab is a list of all the current members and an Add
        button. Clicking the add button will bring up a list of members.
        Multi-select the users that need to be in the impersonation group.

        Once the members are added, create a ManagementScope. Open the Exchange
        Management Shell. Then run the following to create a ManagementScope
        with name "ScopeName" for the security group "SecurityGroupName"
        (this should be on one line, but I wrapped it for readability):
            New-ManagementScope -Name:ScopeName -RecipientRestrictionFilter
            {MemberOfGroup -eq
            "cn=SecurityGroupName,cn=Users,DC=int,DC=tartarus,DC=com"}
        Then create a ManagementRoleAssignment for jclouseau. To create a
        ManagementRoleAssignment named "ManagementRoleAssignmentName" over
        scope "ScopeName" (again this is just one line):
            New-ManagementRoleAssignment -Name:ManagementRoleAssignmentName
            -Role:ApplicationImpersonation -User:jclouseau
            -CustomRecipientWriteScope:ScopeName

        Now jclouseau is set up to impersonate the other users.


        Below is the required structure; identation denotes the heirarchy.
        Some of the folders have a required count, that's
        denoted by having the count in parentheses after the folder name.
        The contents of the emails are defined in the actual code, by adding
        EmailMatchers to requiredEmails.
        Note that if you put html tags in the body (such as changing formatting),
        that is considered text.

          aloner@*
            Inbox
              Rebecca
            Eduardo

          marcher@*
            Inbox
              Anthony
            Barbara

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
        final String keyHeader = "Item ID";

        EmailMatchers requiredEmails = new EmailMatchers(hbase.getFamily());

        /////////////
        //  aloner
        /////////////
        // Inbox
        requiredEmails.add()
                      .subject("Dear Alex")
                      .bodyContains("leave this in your inbox");
        // Rebecca
        requiredEmails.add()
                      .subject("About Rebecca")
                      .bodyContains("She did something");
        // Eduardo
        requiredEmails.add()
                      .subject("About Eduardo")
                      .bodyContains("Something happened to him");

        /////////////
        // marcher
        /////////////
        // Inbox
        requiredEmails.add()
                      .subject("Dear Mary")
                      .bodyContains("Don't rearrange");
        // Anthony
        requiredEmails.add()
                      .subject("About Anthony")
                      .bodyContains("He did something");
        // Barbara
        requiredEmails.add()
                      .subject("About Barbara")
                      .bodyContains("Something happened to her");

        /////////////
        // korganizer
        /////////////
        // Inbox
        requiredEmails.add()
                      .sender("tsender")
                      .subject("Leave it be")
                      .bodyContains("love your inbox clean");
        // child of Inbox
        requiredEmails.add()
                      .subject("To the child of inbox")
                      .bodyContains("child of Inbox");
        // Inbox Jr
        requiredEmails.add()
                      .bodyContains("Inbox Jr")
                      .bodyContains("is getting lonely");
        requiredEmails.add().to("korganizer").subject("For Inbox Jr").bodyContains("Inbox Jr");
        // Drafts
        requiredEmails.add().to("tsender").subject("A draft");
        // Sent Items
        requiredEmails.add()
                      .sender("korganizer")
                      .to("abenjamin")
                      .subject("A message to someone else");
        requiredEmails.add()
                      .bcc("tsender")
                      .to("bkerr")
                      .subject("to whom");
        // Deleted Items
        requiredEmails.add().subject("Whoops").bodyContains("this is trash");
        // Deleted Folder
        requiredEmails.add().bodyContains("Deleted Folder");
        // Topper
        requiredEmails.add()
                      .to("bkerr")
                      .cc("korganizer")
                      .subject("Hey hey Bobbie, throw it in the Topper");
        // Middler
        requiredEmails.add().bodyContains("away this should go into middler, placed neatly.");
        requiredEmails.add().subject("Another middler");
        requiredEmails.add().subject("Yet another in the middler");
        // Middler Jr
        requiredEmails.add().subject("organize away to MJ").bodyContains("Middler Jr");
        // Middler II (0)
        // Middler III
        requiredEmails.add().subject("Forward to Middler III");
        // Middler IV (0)
        // Ms child
        requiredEmails.add().bodyContains("Ms child").bodyContains("nicer than MJ");
        requiredEmails.add()
                      .subject("Super nesting")
                      .bodyContains("Ms child")
                      .bodyContains("wants to be in the loop too.");
        // Page FindItems (29)
        final int findItemsCount = 30;
        for (int i = 1; i < findItemsCount; i++)
        {
            requiredEmails.add()
                          .subject("Page FindItems" + i)
                          .bodyContains("Page FindItems")
                          .bodyContains("#" + i);
        }
        // Page GetItems (11)
        final int getItemsCount = 12;
        for (int i = 1; i < getItemsCount; i++)
        {
            requiredEmails.add()
                          .subject("Page GetItems" + i)
                          .bodyContains("Page GetItems")
                          .bodyContains("#" + i);
        }


        final String exchangeURL = IntegrationTestProperties.getProperty(EXCHANGE_URI_PROPERTY_NAME);
        final String ldapDomain = IntegrationTestProperties.getProperty(LDAP_DOMAIN_PROPERTY_NAME);
        final String ldapConfigEntry = IntegrationTestProperties.getProperty(LDAP_CONFIG_ENTRY_PROPERTY_NAME);

        Auth.authenticateAndDo(new PrivilegedAction<Object>()
        {
            @Override
            public Object run()
            {
                final int findItemsPageSize = 12;
                final int getItemsPageSize = 4;
                MailStore mailStore = new ExchangeMailStore(exchangeURL, 12, 4);
                MailWriter mailWriter = HBaseMailWriter.create(hbase.getTable(), keyHeader, hbase.getFamily());

                try
                {
                    Iterable<String> users = new LdapFetcher(ldapDomain).getPrincipals();
                    Iterable<MailboxItem> mailboxItems = mailStore.getMail(users);
                    Assert.assertTrue(mailboxItems.iterator().hasNext());
                    mailWriter.write(mailboxItems);
                }
                catch (PrincipalFetchException e)
                {
                    e.printStackTrace();
                }
                return null;
            }
        }, ldapConfigEntry);

        // Now prove that everything is in HBase.

        try
        {
            HTableInterface hTable = hbase.getTestingTable();
            Scan scan = createScan(hbase.getFamily(), new String[]{"Subject", "Sender", "Bcc", "Cc", "To", "Body"});
            ResultScanner scanner = hTable.getScanner(scan);
            for (Result result = scanner.next(); result != null; result = scanner.next())
            {
                requiredEmails.match(result);
            }
            requiredEmails.assertEmpty();
        }
        catch (Exception e)
        {
            Assert.fail("Error when attempting to compare.");
        }
    }
}
