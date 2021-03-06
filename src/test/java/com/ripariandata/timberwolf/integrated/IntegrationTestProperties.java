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
package com.ripariandata.timberwolf.integrated;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import org.junit.Assume;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * This rule checks that certain properties are set, and ignores the test if
 * they're not.
 * <br/>
 * You can access the required properties using getProperty().
 * <br/>
 * Some examples of properties that would be handled by this include the port
 * to attach to when hitting a real instance of hbase.
 * <br/>
 * It also prints out to standard err a message so that you know what needs to
 * be set. It would be nice if we could just have an ignore message, but
 * surefire doesn't support that, so we cannot.
  */
public class IntegrationTestProperties implements TestRule
{
    private static final Properties SET_PROPERTIES;

    private static final String FILENAME_PROPERTY_NAME = "test.properties.file";
    private static final String DEFAULT_FILENAME = "testing.properties";

    private static String path;

    private String[] requiredProperties;

    static
    {
        SET_PROPERTIES = new Properties();
        path = null;
        try
        {
            path = System.getProperty(FILENAME_PROPERTY_NAME);
        }
        catch (SecurityException se)
        {
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
                SET_PROPERTIES.loadFromXML(file);
            }
            else
            {
                SET_PROPERTIES.load(file);
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

    /**
     * Creates a new rule with a set of required properties.
     * @param propertyNames the properties that must be set, or the test will
     * be ignored
     */
    public IntegrationTestProperties(final String... propertyNames)
    {
        requiredProperties = propertyNames;
    }

    /**
     * Returns the testing property.
     * @param name the name of the property
     * @return the value of the property or null if the property is not set
     */
    public static String getProperty(final String name)
    {
        return SET_PROPERTIES.getProperty(name);
    }

    @Override
    public Statement apply(final Statement statement, final Description description)
    {
        return new IntegrationPropertiesStatement(description.getClassName() + "." + description.getMethodName(),
                                                  requiredProperties, statement);
    }

    /**
     * A Statement used for IntegrationProperties.
     */
    private class IntegrationPropertiesStatement extends Statement
    {
        private String testName;
        private String[] propertyNames;
        private Statement statement;

        public IntegrationPropertiesStatement(final String methodName, final String[] properties, final Statement base)
        {
            testName = methodName;
            propertyNames = properties;
            statement = base;
        }

        @Override
        public void evaluate() throws Throwable
        {
            ignoreIfMissingProperties();
            statement.evaluate();
        }

        /**
         * Ignores the test (by calling JUnit's Assume methods) if any of the
         * properties is undefined and list the necessary properties in System.err.
         *
         */
        private void ignoreIfMissingProperties()
        {
            boolean ignoreTest = false;
            for (String property : propertyNames)
            {
                if (getProperty(property) == null)
                {
                    ignoreTest = true;
                    break;
                }
            }
            if (ignoreTest)
            {
                StringBuilder sb = new StringBuilder();
                sb.append(" * Ignored Test: ");
                sb.append(testName);
                sb.append("\n *     Requires the properties: ");
                sb.append('\"');
                sb.append(propertyNames[0]);
                sb.append('\"');

                for (int i = 1; i < propertyNames.length - 1; i++)
                {
                    sb.append(", \"");
                    sb.append(propertyNames[i]);
                    sb.append('\"');
                }
                if (propertyNames.length > 1)
                {
                    sb.append(", and \"");
                    sb.append(propertyNames[1]);
                    sb.append("\"");
                }
                sb.append("\n *     Specified in the file: ");
                sb.append(path);
                // Ideally we would be able to just put a message in, but
                // surefire does not respect the ignored message, so we
                // have to print to stderr for the user to see it at all
                System.err.println(sb);
                Assume.assumeTrue(!ignoreTest);
            }
        }
    }
}
