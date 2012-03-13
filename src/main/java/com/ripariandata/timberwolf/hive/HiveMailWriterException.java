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
package com.ripariandata.timberwolf.hive;

import org.slf4j.Logger;

/** Exception thrown on errors while writing data for Hive. */
public class HiveMailWriterException extends RuntimeException
{
    public HiveMailWriterException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Logs a HiveMailWriterException to the appropriate logs.
     * @param logger The logger to use for logging.
     * @param e The HiveMailWriterException to log.
     * @return The HiveMailWriter logged.
     */
    public static HiveMailWriterException log(final Logger logger, final HiveMailWriterException e)
    {
        logger.error(e.getMessage());
        logger.debug("", e);

        return e;
    }
}
