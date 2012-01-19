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
