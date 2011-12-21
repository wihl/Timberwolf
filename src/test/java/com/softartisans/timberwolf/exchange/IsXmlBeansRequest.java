package com.softartisans.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.BaseRequestType;
import org.mockito.ArgumentMatcher;

import static org.mockito.Mockito.argThat;

/**
 * An ArgumentMatcher for
 */
public class IsXmlBeansRequest<T extends BaseRequestType> extends ArgumentMatcher<T>
{
    final T expected;

    public IsXmlBeansRequest(T expected)
    {
        this.expected = expected;
    }

    public static <T extends BaseRequestType> T LikeThis(
            T expected)
    {
        return argThat(new IsXmlBeansRequest<T>(expected));
    }


    @Override
    public boolean matches(Object o)
    {
        if (expected == null)
        {
            return o == null;
        }
        if (o instanceof BaseRequestType)
        {
            T t = (T) o;
            if (expected.xmlText() == null)
            {
                return t.xmlText() == null;
            }
            else
            {
                return expected.xmlText().equals(t.xmlText());
            }
        }
        else
        {
            return false;
        }
    }
}
