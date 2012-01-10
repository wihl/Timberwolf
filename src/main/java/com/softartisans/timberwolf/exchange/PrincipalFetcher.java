package com.softartisans.timberwolf.exchange;

/** This class will return a list of principals we should fetch emails for. */
public interface PrincipalFetcher
{
    Iterable<String> getPrincipals() throws PrincipalFetchException;
}
