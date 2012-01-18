package com.softartisans.timberwolf;

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
    public static final String ISO_8601_FORMAT = "YYYY-MM-dd'T'HH:mm:ssZ";

    private Utilities()
    {
    }

    /**
     * Reads an entire input stream into a string, one line at a time.
     * @param stream the input stream to read in
     * @return the contents of the stream as a string
     * @throws IOException if something goes wrong reading from the input stream
     */
    public static String inputStreamToString(final InputStream stream) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder text = new StringBuilder();
        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            text.append(line);
        }
        return text.toString();
    }
}
