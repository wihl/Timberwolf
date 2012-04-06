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
package com.ripariandata.timberwolf.mail.exchange;

import org.slf4j.Logger;

/** Non-checked exception thrown by ExchangeMailStore iterator. */
public class ExchangeRuntimeException extends RuntimeException
{
    public ExchangeRuntimeException(final String message, final Throwable cause)
    {
        super(message, cause);
    }

    /**
     * Logs a ExchangeRuntimeException to the appropriate logs.
     * @param logger The logger to use for logging.
     * @param e The ExchangeRuntimeException to log.
     * @return The ExchangeRuntimeException logged.
     */
    public static ExchangeRuntimeException log(final Logger logger, final ExchangeRuntimeException e)
    {
        logger.error(e.getMessage());
        logger.debug("", e);

        return e;
    }
}
