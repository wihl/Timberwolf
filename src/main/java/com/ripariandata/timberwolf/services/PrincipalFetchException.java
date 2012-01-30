/**
 * Copyright 2012 Riparian Data
 * http://www.ripariandata.com
 * contact@ripariandata.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ripariandata.timberwolf.services;

import org.slf4j.Logger;

/**
 * This or a derivative of this is thrown should an error occur fetching
 * a complete list of principals from whatever derives PrincipalFetcher.
 */
public class PrincipalFetchException extends Exception
{
    public PrincipalFetchException(final Exception innerException)
    {
        super(innerException);
    }

    public PrincipalFetchException(final String message,
                                   final Exception innerException)
    {
        super(message, innerException);
    }

    /**
     * Logs a PrincipalFetchException to the appropriate logs.
     * @param logger The logger to use for logging.
     * @param e The PrincipalFetchException to log.
     * @return The PrincipalFetchException logged.
     */
    public static PrincipalFetchException log(final Logger logger, final PrincipalFetchException e)
    {
        logger.error(e.getMessage());
        logger.debug("", e);

        return e;
    }
}
