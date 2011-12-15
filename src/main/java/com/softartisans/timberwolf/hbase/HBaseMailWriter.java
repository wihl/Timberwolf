package com.softartisans.timberwolf.hbase;

import com.softartisans.timberwolf.MailWriter;
import com.softartisans.timberwolf.MailboxItem;
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
    private IHBaseTable pMailTable;

    /** The selected MailboxItem header to use as a row key. */
    private String pKeyHeader;

    /** The column family to use for our headers. */
    private byte[] pColumnFamily;

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
        pMailTable = mailTable;
        pKeyHeader = keyHeader;
        pColumnFamily = Bytes.toBytes(columnFamily);
    }

    @Override
    public final void write(final Iterable<MailboxItem> mails)
    {
        for (MailboxItem mailboxItem : mails)
        {
            Put mailboxItemPut = new Put(Bytes.toBytes(
                    mailboxItem.getHeader(pKeyHeader)));

            String[] headerKeys = mailboxItem.getHeaderKeys();

            for (String headerKey : headerKeys)
            {
                mailboxItemPut.add(pColumnFamily, Bytes.toBytes(headerKey),
                        Bytes.toBytes(mailboxItem.getHeader(headerKey)));
            }

            pMailTable.put(mailboxItemPut);

        }
        pMailTable.flush();
    }

}
