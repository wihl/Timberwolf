package com.ripariandata.timberwolf;

import org.joda.time.DateTime;

/**
 * Allows determining when a user was last setUpdateTime and setting that the user has been setUpdateTime.
 */
public interface UserTimeUpdater
{
    /**
     * Determines when the given user was last updated.
     * @return When the user was last updated.
     */
    DateTime lastUpdated(String user);

    /**
     * Sets that the user has been setUpdateTime at this datetime.
     * @param user The user who has been updated.
     * @param dateTime The datetime of the update.
     */
    void setUpdateTime(String user, DateTime dateTime);
}
