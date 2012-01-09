package com.softartisans.timberwolf.exchange;

public interface PrincipalFetcher
{
    Iterable<String> getPrincipals() throws PrincipalFetchException;
}
