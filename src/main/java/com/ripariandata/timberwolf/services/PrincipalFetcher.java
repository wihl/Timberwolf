package com.ripariandata.timberwolf.services;

/** This class will return a list of principals we should fetch emails for. */
public interface PrincipalFetcher
{
    Iterable<String> getPrincipals() throws PrincipalFetchException;
}
