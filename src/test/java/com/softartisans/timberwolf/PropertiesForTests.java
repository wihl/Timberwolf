package com.softartisans.timberwolf;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

/**
 * This class is used for managing the file associated with properties
 * necessary for testing.
 *
 * Some examples of properties that would be handled by this include the port
 * to attach to when hitting a real instance of hbase.
 *
 * If a test is ignored because it is missing properties it should list all the
 * necessary properties in the ignore message, along with getIgnoreMessage()
 */
public class PropertiesForTests
{
    private static final Properties properties;

    private static final String FILENAME_PROPERTY_NAME = "test.properties.file";
    private static final String DEFAULT_FILENAME = "testing.properties";

    static
    {
        properties = new Properties();
        String path = null;
        try
        {
            path = System.getProperty(FILENAME_PROPERTY_NAME);
        } catch (SecurityException se) {
            System.err.println("Permission denied for accessing property: \"" + FILENAME_PROPERTY_NAME
                               + "\"; using default filename \"testing.properties\"");
        }
        if (path == null)
        {
            path = DEFAULT_FILENAME;
        }
        try
        {
            FileInputStream file = new FileInputStream(path);
            if (path.endsWith(".xml"))
            {
                properties.loadFromXML(file);
            }
            else
            {
                properties.load(file);
            }
        }
        catch (SecurityException e)
        {
            System.err.println("Permission denied for accessing properties file: \"" + path + "\"");
        }
        catch (FileNotFoundException e)
        {
            System.err.println("No properties file: \"" + path + "\"; some tests will be ignored");
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Could not read testing properties from file \"" + path
                               + "\" due to invalid Unicode escape sequence; " + e.toString());
        }
        catch (InvalidPropertiesFormatException e)
        {
            System.err.println("Testing properties in file \"" + path + "\" are invalid: " + e.toString());
        }
        catch (IOException e)
        {
            System.err.println("Could not read testing properties from file \"" + path + "\"; " + e.toString());
        }
        catch (Exception e)
        {
            System.err.println("Unexpected error reading testing properties from file \"" + path + "\": ");
            e.printStackTrace();
        }
    }

    public static String getProperty(String name)
    {
        return properties.getProperty(name);
    }

}
