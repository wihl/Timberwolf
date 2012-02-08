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
package com.ripariandata.timberwolf;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * A simple class with some utility methods.
 */
public final class Utilities
{
    /**
     * A format string that produces IS0 8601-compatible date strings.  Suitable
     * for use with org.joda.time.DateTime.toString().  For more information on
     * ISO 8601, see http://en.wikipedia.org/wiki/ISO_8601.
     */
    public static final String ISO_8601_FORMAT = "YYYY-MM-dd'T'HH:mm:ss";

    private Utilities()
    {
    }

    /**
     * Reads an entire input stream into a string, one line at a time.
     *
     * @param stream the input stream to read in
     * @param charset the charset that the input stream is encoded in
     * @return the contents of the stream as a string
     * @throws IOException if something goes wrong reading from the input stream
     */
    public static String inputStreamToString(final InputStream stream, final String charset) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, charset));
        StringBuilder text = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            text.append(line);
        }
        return text.toString();
    }
}
