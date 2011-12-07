package com.softartisans.timberwolf;

import org.apache.hadoop.hbase.client.HTableInterface;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Iterator;

public class HBaseMailWriter implements MailWriter 
{
    private HTableInterface _mailTable;
    private String _keyHeader;
    private byte[] _columnFamily;
    private static final String _defaultColumnFamily = "h";
    private static final String _defaultKeyHeader = "dkh";

    public HBaseMailWriter(HTableInterface mailTable)
    {
        this(mailTable, _defaultKeyHeader, _defaultColumnFamily);
    }
    
    public HBaseMailWriter(HTableInterface mailTable, String keyHeader)
    {
        this(mailTable, keyHeader, _defaultColumnFamily);
    }
    
    public HBaseMailWriter(HTableInterface mailTable, String keyHeader, String columnFamily)
    {
        _mailTable = mailTable;
        _keyHeader = keyHeader;
        _columnFamily = Bytes.toBytes(columnFamily);
        
    }
       
    @Override
    public void write(Iterator<MailboxItem> mails)
    {
        while(mails.hasNext()) 
        {
            MailboxItem mailboxItem = mails.next();
            
            Put mailboxItemPut = new Put(Bytes.toBytes(mailboxItem.getHeader(_keyHeader)));
            
            String[] headerKeys = mailboxItem.getHeaderKeys();
            
            for( String headerKey : headerKeys)
            {
                mailboxItemPut.add(_columnFamily, Bytes.toBytes(headerKey),
                        Bytes.toBytes(mailboxItem.getHeader(headerKey)));
            }

            try
            {
                _mailTable.put(mailboxItemPut);
            }
            catch(IOException e)
            {
                // TODO: Log error.
            }
        }
    }

}
