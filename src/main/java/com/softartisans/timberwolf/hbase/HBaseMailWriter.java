package com.softartisans.timberwolf.hbase;

import com.softartisans.timberwolf.MailWriter;
import com.softartisans.timberwolf.MailboxItem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class writes a list of MailboxItems to an HTableInterface.
 */
public class HBaseMailWriter implements MailWriter
{
    /** Our general purpose logger. */
    private static Logger logger =
            LoggerFactory.getLogger(HBaseMailWriter.class);

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

    public HBaseMailWriter(final IHBaseTable mailTable)
    {
        this(mailTable, DEFAULT_KEY_HEADER, DEFAULT_COLUMN_FAMILY);
    }

    public HBaseMailWriter(final IHBaseTable mailTable,
                           final String keyHeader)
    {
        this(mailTable, keyHeader, DEFAULT_COLUMN_FAMILY);
    }

    public HBaseMailWriter(final IHBaseTable mailTable,
                           final String keyHeader,
                           final String columnFamily)
    {
        this.mailTable = mailTable;
        this.keyHeader = keyHeader;
        this.columnFamily = Bytes.toBytes(columnFamily);
    }

    /**
     * Factory method for creating a HBaseMailWriter for a specific HBase
     * instance and table, with properties.
     * @param hbase The HBase manager instance to use for this connection.
     * @param tableName The name of the table to store mail.
     * @param keyHeader The mail header to use as a row key in HBase.
     * @param columnFamily The column family to use for storing headers in
     *                     HBase.
     * @return A HBaseMailWriter utilizing the above parameters.
     */
    public static HBaseMailWriter create(final HBaseManager hbase,
                                         final String tableName,
                                         final String keyHeader,
                                         final String columnFamily)
    {
       return new HBaseMailWriter(hbase.getTable(tableName), keyHeader,
                columnFamily);
    }

    /**
     * Writes the iterable list of MailboxItems to the underlying HBase table.
     * @param mails The iterable list of MailBoxItems.
     */
    @Override
    public final void write(final Iterable<MailboxItem> mails)
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
