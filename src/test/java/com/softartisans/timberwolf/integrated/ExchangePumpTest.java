package com.softartisans.timberwolf.integrated;

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
        String exchangeURL = IntegrationTestProperties.getProperty(EXCHANGE_URI_PROPERTY_NAME);
        RequiredUser bkerr = new RequiredUser("bkerr");
        RequiredFolder folder1 = bkerr.addFolderToInbox("folder1");
        folder1.add(email("bkerr"), "My first email", "The body of said email");
        folder1.addFolder("folderA");
        folder1.addFolder("folderD");
//        RequiredFolder folder2 = bkerr.addFolderToRoot("folder2");
//        folder2.addFolder("folderB");
//        folder2.addFolder("folderC");


        ExchangePump pump = new ExchangePump(exchangeURL, "bkerr");
        bkerr.initialize(pump);

        bkerr.sendEmail(pump);
        bkerr.moveEmails(pump);
    }

}
