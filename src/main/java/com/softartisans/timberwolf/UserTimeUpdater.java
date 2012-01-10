package com.softartisans.timberwolf;

import org.joda.time.DateTime;

/**
 * Allows determining when a user was last updated and setting that the user has been updated.
 */
public interface UserTimeUpdater
{
    /**
     * Determines when the given user was last updated.
     * @return When the user was last updated.
     */
    DateTime lastUpdated(String user);

    /**
     * Sets that the user has been updated at this datetime.
     * @param user The user who has been updated.
     * @param dateTime The datetime of the update.
     */
    void updated(String user, DateTime dateTime);
}
