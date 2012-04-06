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
package com.ripariandata.timberwolf.writer.console;

import com.ripariandata.timberwolf.mail.MailboxItem;
import com.ripariandata.timberwolf.writer.MailWriter;

/**
 * Writes a series of mails to the console, for debugging purposes.
 *
 */
public final class ConsoleMailWriter implements MailWriter
{
    private static final String BETWEEN_MAIL =
            "===========================================================";

    @Override
    public void write(final Iterable<MailboxItem> mails)
    {
        for (final MailboxItem mail : mails)
        {
            System.out.println(BETWEEN_MAIL);
            for (String key : mail.getHeaderKeys())
            {
                System.out.print(key);
                System.out.print(": ");

                String value = mail.getHeader(key);
                System.out.println(value);
            }
        }
    }
}
