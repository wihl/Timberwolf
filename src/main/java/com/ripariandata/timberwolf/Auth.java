package com.ripariandata.timberwolf;

import com.sun.security.auth.callback.TextCallbackHandler;

import java.security.PrivilegedAction;

import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

/** Helper class for running some code as an authenticated user. */
public final class Auth
{
    private Auth()
    {
    }

    /**
     * Authenticates the given privileged action with the config entry, and run it.
     *
     * @param action the action to run when authenticated
     * @param authenticationEntry the java login configuration entry.
     * @param <T> the type that the action's run method returns
     * @return the value that action's run method returns
     * @throws LoginException if this fails to login
     */
    public static <T> T authenticateAndDo(final PrivilegedAction<T> action,
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
