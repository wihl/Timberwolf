package com.softartisans.timberwolf.integrated;

import com.cloudera.alfredo.client.AuthenticationException;
import com.softartisans.timberwolf.MailStore;
import com.softartisans.timberwolf.MailWriter;
import com.softartisans.timberwolf.MailboxItem;
import com.softartisans.timberwolf.exchange.ExchangeMailStore;
import com.softartisans.timberwolf.hbase.HBaseConfigurator;
import com.softartisans.timberwolf.hbase.HBaseMailWriter;
import com.softartisans.timberwolf.hbase.HBaseManager;
import com.softartisans.timberwolf.hbase.IHBaseTable;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

/**
 * Overall integration testing for timberwolf.
 */
public class TestIntegration {

    private static final String ZOO_KEEPER_QUORUM_PROPERTY_NAME = "ZooKeeperQuorum";
    private static final String ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME = "ZooKeeperClientPort";

    @Rule
    public IntegrationTestProperties properties = new IntegrationTestProperties(ZOO_KEEPER_QUORUM_PROPERTY_NAME,
            ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME);

    private Get createGet(String row, String columnFamily, String[] headers)
    {
            Get get = new Get(Bytes.toBytes(row));
            for( String header : headers)
            {
                get.addColumn(Bytes.toBytes(columnFamily), Bytes.toBytes(header));
            }
        return get;
    }

    @Test
    public void testIntegrationNoCLI()
    {
        String tableName = "testIntegrationNoCLI";
        String columnFamily = "h";
        String keyHeader = "Item ID";

        HBaseManager hbase = new HBaseManager(properties.getProperty(ZOO_KEEPER_QUORUM_PROPERTY_NAME),
                properties.getProperty(ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME));

        List<String> columnFamilies = new ArrayList<String>();
        columnFamilies.add(columnFamily);
        IHBaseTable table = hbase.createTable(tableName, columnFamilies);

        String exchangeURL = "https://devexch01.int.tartarus.com/ews/exchange.asmx";

        MailStore mailStore = new ExchangeMailStore(exchangeURL);
        MailWriter mailWriter = HBaseMailWriter.create(table, keyHeader, columnFamily);

        try
        {
            Iterable<MailboxItem> mailboxItems = mailStore.getMail();
            Assert.assertTrue(mailboxItems.iterator().hasNext());
            mailWriter.write(mailboxItems);
        }
        catch (IOException e)
        {
            Assert.fail("Error writing mail!");
        }
        catch (AuthenticationException e)
        {
            Assert.fail("Error authenticating to Exchange");
        }

        // Now prove that everything is in HBase.

        Configuration configuration = HBaseConfigurator.createConfiguration(
                properties.getProperty(ZOO_KEEPER_QUORUM_PROPERTY_NAME),
                properties.getProperty(ZOO_KEEPER_CLIENT_PORT_PROPERTY_NAME));
        try
        {
            HTableInterface hTable = new HTable(configuration, tableName);
            Iterable<MailboxItem> mails = mailStore.getMail();

            for (MailboxItem mail : mails)
            {
                Get get = createGet(mail.getHeader(keyHeader),columnFamily,mail.getHeaderKeys());
                Result result = hTable.get(get);
                for(String header : mail.getHeaderKeys())
                {
                    String tableValue = Bytes.toString(result.getValue(Bytes.toBytes(columnFamily),
                            Bytes.toBytes(header)));
                    Assert.assertEquals(mail.getHeader(header),tableValue);
                }
            }
        }
        catch (Exception e)
        {
            Assert.fail("Error when attempting to compare.");
        }

        hbase.deleteTable(tableName);
    }
}
