package com.softartisans.timberwolf.smock;

import junit.framework.Assert;
import org.apache.axis2.util.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * This is the glue that ties expectation and responses together
 */
class Communication
{

    private SmockBase.Resource request;
    private SmockBase.Resource response;
    private ByteArrayOutputStream output;

    public void setRequest(SmockBase.Resource resource)
    {
        this.request = resource;
    }

    public void setResponse(SmockBase.Resource resource)
    {
        this.response = resource;
    }

    /** Get the stream to which axis should write the request. */
    public OutputStream getOutputStream()
    {
        output = new ByteArrayOutputStream();
        return output;
    }

    /**
     * Assert that the request axis wrote is the same as the one passed
     * into expect.
     */
    public void assertRequest()
    {
        InputStream input = null;
        ByteArrayOutputStream expected = null;
        try
        {
            input = request.getStream();
            if (input == null)
            {
                Assert.fail("Could not find request resource: " + request);
            }
            expected = new ByteArrayOutputStream();
            IOUtils.copy(input, expected, false);
            Assert.assertEquals(expected.toString("UTF-8"),
                                output.toString("UTF-8"));
        }
        catch (UnsupportedEncodingException e)
        {
            e.printStackTrace();
            Assert.fail(e.toString());
        }
        catch (IOException e)
        {
            e.printStackTrace();
            Assert.fail(e.toString());
        }
        finally
        {
            safeClose(input);
            safeClose(expected);
        }
    }

    /** helper method to make assertRequest cleaner, closes the Closeable */
    private void safeClose(Closeable input)
    {
        if (input != null)
        {
            try
            {
                input.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
                Assert.fail(e.toString());
            }
        }
    }

    /** Get the stream that axis should read the mocked response from */
    public InputStream getInputStream()
    {
        if (response == null)
        {
            return new ByteArrayInputStream(new byte[0]);
        }
        else
        {
            InputStream returnValue = response.getStream();
            if (returnValue == null)
            {
                Assert.fail("Could not find response resource: "
                            + response);
            }
            return returnValue;
        }
    }
}
