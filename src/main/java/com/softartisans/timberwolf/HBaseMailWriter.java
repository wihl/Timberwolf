package com.softartisans.timberwolf;

import java.util.Iterator;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.*;

public class HBaseMailWriter implements MailWriter{
    
    @Override
    public void write(Iterator<Email> mails) {
        System.out.println("HBaseMailWriter.write");
    }

}
