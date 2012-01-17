package com.softartisans.timberwolf;

import com.sun.security.auth.callback.TextCallbackHandler;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/**
 * This simple class will perform the given privileged action using
 * the given authentication Entry. The entry corresponds to a java
 * login configuration.
 *
 * @param <T> This is the return type of the PrivilegedAction
 */
public class Auth<T>
{
    T authenticateAndDo(final PrivilegedAction<T> action,
                        final String authenticationEntry)
            throws LoginException
    {
        LoginContext lc = new LoginContext(authenticationEntry,
                                           new TextCallbackHandler());
        // Attempt authentication
        // We might want to do this in a "for" loop to give
        // user more than one chance to enter correct username/password
        lc.login();

        try
        {
            return Subject.doAs(lc.getSubject(), action);
        }
        finally
        {
            lc.logout();
        }
    }
}
