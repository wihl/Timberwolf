package com.softartisans.timberwolf;

import java.util.Iterator;

public class HBaseMailWriter implements MailWriter{
    
    @Override
    public void write(Iterator<Email> mails) {
        System.out.println("HBaseMailWriter.write");
    }

}
