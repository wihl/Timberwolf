package com.ripariandata.timberwolf.integrated;

import com.microsoft.schemas.exchange.services.x2006.types.DistinguishedFolderIdNameType;
import com.ripariandata.timberwolf.Auth;
import com.ripariandata.timberwolf.MailStore;
import com.ripariandata.timberwolf.MailWriter;
import com.ripariandata.timberwolf.MailboxItem;
import com.ripariandata.timberwolf.NoopUserTimeUpdater;
import com.ripariandata.timberwolf.exchange.ExchangeMailStore;
import com.ripariandata.timberwolf.exchange.ExchangePump;
import com.ripariandata.timberwolf.exchange.ExchangePump.FailedToCreateMessage;
import com.ripariandata.timberwolf.exchange.ExchangePump.FailedToFindMessage;
import com.ripariandata.timberwolf.exchange.ExchangePump.FailedToMoveMessage;
import com.ripariandata.timberwolf.exchange.RequiredFolder;
import com.ripariandata.timberwolf.hbase.HBaseMailWriter;
import com.ripariandata.timberwolf.services.LdapFetcher;
import com.ripariandata.timberwolf.services.PrincipalFetchException;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Iterator;
import javax.security.auth.login.LoginException;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Scan;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Overall integration testing, from exchange server to hbase.
 */
public class TestIntegration
{
    private static final String EXCHANGE_URI_PROPERTY_NAME = "ExchangeURI";
    private static final String EXCHANGE_USER1_PROPERTY_NAME = "ExchangeUser1";
    private static final String EXCHANGE_USER2_PROPERTY_NAME = "ExchangeUser2";
    private static final String EXCHANGE_USER3_PROPERTY_NAME = "ExchangeUser3";
    private static final String EXCHANGE_SENDER_PROPERTY_NAME = "ExchangeSender";
    private static final String EXCHANGE_IGNORED_USER_PROPERTY_NAME = "ExchangeIgnoredUser";

    private static final String LDAP_DOMAIN_PROPERTY_NAME = "LdapDomain";
    private static final String LDAP_CONFIG_ENTRY_PROPERTY_NAME = "LdapConfigEntry";

    RequiredUser user1;
    RequiredUser user2;
    RequiredUser user3;

    @Rule
    public IntegrationTestProperties properties = new IntegrationTestProperties(EXCHANGE_URI_PROPERTY_NAME,
                                                                                EXCHANGE_USER1_PROPERTY_NAME,
                                                                                EXCHANGE_USER2_PROPERTY_NAME,
                                                                                EXCHANGE_USER3_PROPERTY_NAME,
                                                                                EXCHANGE_SENDER_PROPERTY_NAME,
                                                                                EXCHANGE_IGNORED_USER_PROPERTY_NAME,
                                                                                LDAP_DOMAIN_PROPERTY_NAME,
                                                                                LDAP_CONFIG_ENTRY_PROPERTY_NAME);

    @Rule
    public HTableResource hbase = new HTableResource();
    ExchangePump pump;

    String exchangeURL;
    String ldapDomain;
    String ldapConfigEntry;

    final String keyHeader = "Item ID";

    // The emails of this user are not checked
    String senderUsername;
    String senderEmail;

    // The emails of this user are not checked
    String ignoredUsername;
    String ignoredEmail;

    String username1;
    String email1;
    String username2;
    String username3;

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

    private String email(String username)
    {
        return username + "@" + IntegrationTestProperties.getProperty(LDAP_DOMAIN_PROPERTY_NAME);
    }

    private void removeUsers(final Iterable<String> users, final String ignoredUsername1, final String ignoredUsername2)
    {
        Iterator<String> p = users.iterator();
        // We are going to skip over the sender username to avoid double
        // counting emails
        while (p.hasNext())
        {
            String user = p.next();
            if (user.equals(ignoredUsername1))
            {
                p.remove();
            }
            else if (user.equals(ignoredUsername2))
            {
                p.remove();
            }
        }
    }

    private void runForEmails(final String exchangeURL, final String ldapDomain, final String ldapConfigEntry,
                              final String keyHeader, final String senderUsername, final String ignoredUsername,
                              final RequiredUser user1, final RequiredUser user2, final RequiredUser user3,
                              final ExchangePump pump)
            throws ExchangePump.FailedToCreateMessage, ExchangePump.FailedToFindMessage,
                   ExchangePump.FailedToMoveMessage, LoginException, IOException
    {
        final ExpectedEmails expectedEmails;
        user1.initialize(pump);
        user2.initialize(pump);
        user3.initialize(pump);

        user1.sendEmail(pump);
        user2.sendEmail(pump);
        user3.sendEmail(pump);

        user1.moveEmails(pump);
        user2.moveEmails(pump);
        user3.moveEmails(pump);

        if (hbase.getTable() == null)
        {
            hbase.regetTable();
        }

        Auth.authenticateAndDo(new PrivilegedAction<Object>()
        {
            @Override
            public Object run()
            {

                MailStore mailStore = new ExchangeMailStore(exchangeURL, 12, 4);
                MailWriter mailWriter = HBaseMailWriter.create(hbase.getTable(), keyHeader, hbase.getFamily());

                try
                {
                    Iterable<String> users = new LdapFetcher(ldapDomain).getPrincipals();
                    removeUsers(users, senderUsername, ignoredUsername);
                    System.out.print("Users: ");
                    System.out.println(StringUtils.join(users.iterator(), ", "));
                    // TODO: actually pass something useful here
                    Iterable<MailboxItem> mailboxItems = mailStore.getMail(users, new NoopUserTimeUpdater());
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

        expectedEmails = new ExpectedEmails();
        expectedEmails.require(user1, hbase);
        expectedEmails.require(user2, hbase);
        expectedEmails.require(user3, hbase);
        expectedEmails.checkHbase();
    }

    @Before
    public void SetupUsers() throws FailedToCreateMessage, FailedToFindMessage, FailedToMoveMessage
    {
        exchangeURL = IntegrationTestProperties.getProperty(EXCHANGE_URI_PROPERTY_NAME);
        ldapDomain = IntegrationTestProperties.getProperty(LDAP_DOMAIN_PROPERTY_NAME);
        ldapConfigEntry = IntegrationTestProperties.getProperty(LDAP_CONFIG_ENTRY_PROPERTY_NAME);

        senderUsername = IntegrationTestProperties.getProperty(EXCHANGE_SENDER_PROPERTY_NAME);
        senderEmail = email(senderUsername);

        ignoredUsername = IntegrationTestProperties.getProperty(EXCHANGE_IGNORED_USER_PROPERTY_NAME);
        ignoredEmail = email(ignoredUsername);

        username1 = IntegrationTestProperties.getProperty(EXCHANGE_USER1_PROPERTY_NAME);
        email1 = email(username1);
        username2 = IntegrationTestProperties.getProperty(EXCHANGE_USER2_PROPERTY_NAME);
        username3 = IntegrationTestProperties.getProperty(EXCHANGE_USER3_PROPERTY_NAME);


        user1 = new RequiredUser(username1, ldapDomain);
        user2 = new RequiredUser(username2, ldapDomain);
        user3 = new RequiredUser(username3, ldapDomain);
    }

    @After
    public void DeleteEmails()
    {
        if (pump != null)
        {
            if (user1 != null)
            {
                user1.deleteEmails(pump);
                user1 = null;
            }
            if (user2 != null)
            {
                user2.deleteEmails(pump);
                user2 = null;
            }
            if (user3 != null)
            {
                user3.deleteEmails(pump);
                user3 = null;
            }
            pump = null;
        }
    }

    @Test
    public void testIntegrationNoCLI()
            throws PrincipalFetchException, LoginException, IOException, ExchangePump.FailedToCreateMessage,
                   ExchangePump.FailedToFindMessage, ExchangePump.FailedToMoveMessage
    {
        /*
        This test tests getting emails from an exchange server, and the breadth
        of what that entails, and then putting it all in Exchange. It depends
        on a certain set of users, specified in the testing.properties. The
        test than creates a bunch of folders in those user accounts, fills them
        with email, runs timberwolf to get the messages into hbase, and then
        asserts that all the created messages are in hbase.
        After the test it deletes ALL content from the user accounts.
        The authenticated user account must be able to impersonate all the
        required user accounts specified in the properties.
         */
        /////////////
        // User #1
        /////////////
        user1.addToInbox("Leave it be", "Even though you love your inbox clean").from(senderEmail);
        user1.addFolderToInbox("child of Inbox")
             .add("To the child of inbox", "here is the body of the email in the child of Inbox");
        RequiredFolder inboxJr = user1.addFolderToInbox("Inbox Jr");
        inboxJr.add("books","Inbox Jr is getting lonely over here");
        inboxJr.add("For Inbox Jr","some sort of body here");
        user1.addDraft(senderEmail,"A draft", "with something I'll never tell you");
        user1.addSentItem(senderEmail, "A message to semone else", "you can tell, because of the to field");
        user1.addSentItem(ignoredEmail, "To whom", "is this email going").bcc(senderEmail);
        user1.addToDeletedItems("Whoops","This is going in the trash");
        user1.addFolder(DistinguishedFolderIdNameType.DELETEDITEMS,"Deleted folder")
             .add("Uh oh", "this is going in the recycling bin, which we're throwing out");
        RequiredFolder topper = user1.addFolderToRoot("Topper");
        topper.add("Hey hey Bobby McGee", "Makes me think of that Janis Joplin song").to(ignoredEmail).cc(email1);
        RequiredFolder middler = topper.addFolder("Middler");
        middler.add("Move it", "Away this should go into middler, placed neatly there for all to see");
        middler.add("Another middler email", "that has a super boring body");
        middler.add("Yet another in the middler folder", "oh so many emails");
        RequiredFolder middlerJr = middler.addFolder("Middler Jr");
        middlerJr.add("organize away to MJ", "this will be moved to middler Jr");
        RequiredFolder middlerIII = middlerJr.addFolder("Middler II").addFolder("Middler III");
        middlerIII.add("Forward this on to Middler III", "where it shall be left");
        RequiredFolder msChild = middlerIII.addFolder("Middler IV").addFolder("Ms child");
        msChild.add("Ms child", "is way nicer than MJ");
        msChild.add("Super nesting", "The child of Ms child is so deep");
        RequiredFolder findItems = user1.addFolderToRoot("Page FindItems");
        // Page FindItems (29)
        for (int i = 0; i < 29; i++)
        {
            findItems.add("Page FindItems" + (i+1), "Page FindItems #" + (i+1));
        }
        RequiredFolder getItems = user1.addFolderToRoot("Page GetItems");
        // Page GetItems (11)
        for (int i = 0; i < 11; i++)
        {
            getItems.add("Page GetItems" + (i+1), "Page GetItems #" + (i+1));
        }

        /////////////
        //  User #2
        /////////////
        user2.addToInbox("Dear Alex", "Leave this bad boy in your inbox");
        user2.addFolderToInbox("Rebecca")
             .add("About Rebecca", "She did something");
        user2.addFolderToRoot("Eduardo")
             .add("Concerning Eduardo", "Something happened to him");

        /////////////
        // User #3
        /////////////
        user3.addToInbox("Dear Mary", "Don't rearrange this email");
        user3.addFolderToInbox("Anthony")
             .add("About Anthony", "He did something");
        user3.addFolderToRoot("Barbara")
             .add("Concerning Barbara", "Something happened to her");

        pump = new ExchangePump(exchangeURL, senderUsername);
        runForEmails(exchangeURL, ldapDomain, ldapConfigEntry, keyHeader, senderUsername, ignoredUsername, user1, user2,
                     user3,
                     pump);

        user1.nextRun();
        user2.nextRun();
        user3.nextRun();

        // TODO actually do that
        user1.addToInbox("An email after the first one", "A new email");
        middlerJr.add("A new email in the middler Jr", "The body for this new email");
        RequiredFolder newFolder = middlerJr.addFolder("New folder");
        newFolder.add("this new email is in a new folder", "and it has a new body");

        runForEmails(exchangeURL, ldapDomain, ldapConfigEntry, keyHeader, senderUsername, ignoredUsername, user1, user2,
                     user3,
                     pump);
    }


}
