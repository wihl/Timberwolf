package com.softartisans.timberwolf.integrated;

import java.security.PrivilegedAction;

import junit.framework.Assert;

import com.softartisans.timberwolf.Auth;
import com.softartisans.timberwolf.exchange.ExchangePump;
import com.softartisans.timberwolf.exchange.RequiredFolder;
import org.junit.Rule;
import org.junit.Test;

/**
 * Temporary class for trying out the ExchangePump before putting it in the
 * integration test.
 */
public class ExchangePumpTest
{
    private static final String EXCHANGE_URI_PROPERTY_NAME = "ExchangeURI";

    private static final String LDAP_DOMAIN_PROPERTY_NAME = "LdapDomain";
    private static final String LDAP_CONFIG_ENTRY_PROPERTY_NAME = "LdapConfigEntry";
    @Rule
    public IntegrationTestProperties properties = new IntegrationTestProperties(EXCHANGE_URI_PROPERTY_NAME,
                                                                                LDAP_DOMAIN_PROPERTY_NAME,
                                                                                LDAP_CONFIG_ENTRY_PROPERTY_NAME);

    private String email(String username)
    {
        return username + "@" + IntegrationTestProperties.getProperty(LDAP_DOMAIN_PROPERTY_NAME);
    }

    @Test
    public void testCreateFolders() throws Exception
    {
        final String exchangeURL = IntegrationTestProperties.getProperty(EXCHANGE_URI_PROPERTY_NAME);
        final RequiredUser bkerr = new RequiredUser("bkerr", IntegrationTestProperties.getProperty(LDAP_DOMAIN_PROPERTY_NAME));
        RequiredFolder folder1 = bkerr.addFolderToInbox("folder1");
        folder1.add("My first email", "The body of said email");
        folder1.add("My second email", "The body of said email");
        RequiredFolder folderA = folder1.addFolder("folderA");
        folderA.add("Another email", "a body of mine");
        RequiredFolder folderD = folder1.addFolder("folderD");
        folderD.add("carbon copy", "cc bkerr").cc(email("bkerr"));
        folderD.add("blind carbon copy", "bcc to him").bcc(email("bkerr")).to("dkramer");

        bkerr.addToInbox("With a fox", "sam i am");
        bkerr.addToDeletedItems("gone", "with the wind");
        bkerr.addDraft(email("dkramer"), "I'm unsure", "as to what I will say");
        bkerr.addSentItem(email("dkramer"), "Hey dkramer", "this is the body");



//        RequiredFolder folder2 = bkerr.addFolderToRoot("folder2");
//        folder2.addFolder("folderB");
//        folder2.addFolder("folderC");


        Auth.authenticateAndDo(new PrivilegedAction<Integer>() {
            public Integer run() {
                ExchangePump pump = new ExchangePump(exchangeURL, "bkerr");
                try
                {
                    bkerr.initialize(pump);

                    bkerr.sendEmail(pump);
                    bkerr.moveEmails(pump);
                }
                catch (Exception e)
                {
                    Assert.fail("Exception was thrown: " + e.getMessage());
                }
                finally
                {
                    bkerr.deleteEmails(pump);
                }
                return 0;
            }
        }, "Timberwolf");
    }

}
