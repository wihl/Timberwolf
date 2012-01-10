package com.softartisans.timberwolf.hbase;

import com.softartisans.timberwolf.MailWriter;
import com.softartisans.timberwolf.MailboxItem;

import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class writes a list of MailboxItems to an IHBaseTable.
 */
public final class HBaseMailWriter implements MailWriter
{
    /** Our general purpose logger. */
    private static final Logger LOG = LoggerFactory.getLogger(HBaseMailWriter.class);

    /** The HTableInterface to store MailboxItems into. */
    private IHBaseTable mailTable;

    /** The selected MailboxItem header to use as a row key. */
    private String keyHeader;

    /** The column family to use for our headers. */
    private byte[] columnFamily;

    /** The default column family to use if left unspecified. */
    private static final String DEFAULT_COLUMN_FAMILY = "h";

    /** The default header, whose value will be used as a rowkey. */
    private static final String DEFAULT_KEY_HEADER = "Item ID";

    private HBaseMailWriter(final IHBaseTable table,
                            final String mailboxItemKeyHeader,
                            final String hbaseColumnFamily)
    {
        this.mailTable = table;
        this.keyHeader = mailboxItemKeyHeader;
        this.columnFamily = Bytes.toBytes(hbaseColumnFamily);
    }

    /**
     * Creates an HBaseMailWriter with the specified settings. If the table
     * specified by tableName does not currently exist, it will be created
     * with the specified columnFamily. Currently, it's not a great idea to
     * make multiple HBaseMailWriters to the same underlying instance.
     * @param quorum The ZooKeeper quorum.
     * @param clientPort The ZooKeeper client port.
     * @param tableName The table to connect to.
     * @param keyHeader The MailboxItem header to use as a row key.
     * @param columnFamily The column family to add mail headers to.
     * @return A new HBaseMailWriter instance with the specified settings.
     */
    public static MailWriter create(final String quorum,
                                         final String clientPort,
                                         final String tableName,
                                         final String keyHeader,
                                         final String columnFamily)
    {
        HBaseManager hbase = new HBaseManager(quorum, clientPort);

        List<String> columnFamilies = new ArrayList<String>();
        columnFamilies.add(columnFamily);

        if (!hbase.tableExists(tableName))
        {
            hbase.createTable(tableName, columnFamilies);
        }

        IHBaseTable table = hbase.getTable(tableName);
        return new HBaseMailWriter(table, keyHeader, columnFamily);
    }

    /**
     * Creates an HBaseMailWriter with the specified settings.
     * @param table The IHBaseTable to write to.
     * @param keyHeader The MailboxItem header to use as a row key.
     * @param columnFamily The column family to add mail headers to.
     * @return A new HBaseMailWriter instance with the specified settings.
     */
    public static MailWriter create(final IHBaseTable table,
                                         final String keyHeader,
                                         final String columnFamily)
    {
        return new HBaseMailWriter(table, keyHeader, columnFamily);
    }

    /**
     * Writes the iterable list of MailboxItems to the underlying HBase table.
     * @param mails The iterable list of MailBoxItems.
     */
    @Override
    public void write(final Iterable<MailboxItem> mails)
    {
        for (MailboxItem mailboxItem : mails)
        {
            Put mailboxItemPut = new Put(Bytes.toBytes(
                    mailboxItem.getHeader(keyHeader)));

            String[] headerKeys = mailboxItem.getHeaderKeys();

            for (String headerKey : headerKeys)
            {
                mailboxItemPut.add(columnFamily, Bytes.toBytes(headerKey),
                        Bytes.toBytes(mailboxItem.getHeader(headerKey)));
            }

            mailTable.put(mailboxItemPut);
        }
        mailTable.flush();
    }

}
