package com.softartisans.timberwolf;

public interface MailStore {
    Email[] getMail(String user);
}