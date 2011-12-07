package com.softartisans.timberwolf;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.IOException;
import java.util.Iterator;

public class HBaseMailWriter implements MailWriter 
{
    private final String _tableName = "Emails";
    private final String _columnName = "h";
    private final String _bodyQualifier = "b";
    
    @Override
    public void write(Iterator<Email> mails)
    {
        HTable emailTable;
        try 
        {
            Configuration configuration = HBaseConfiguration.create();
            emailTable = new HTable(_tableName);
        }
        catch (IOException ioex)
        {
            // TODO: Log exception.
        }

        if (emailTable != null)
        {

        }

        while(mails.hasNext()) 
        {
            Email email = mails.next();

            Put emailPut = new Put(Bytes.toBytes(email.getSender()));
            emailPut.add(Bytes.toBytes(_columnName), Bytes.toBytes(_bodyQualifier), Bytes.toBytes(email.getBody()));
            
            emailTable.add(emailPut);
        }
    }

}
