package com.softartisans.timberwolf;

import java.util.Iterator;

public interface MailStore {
    Iterator<Email> getMail(String user);
}