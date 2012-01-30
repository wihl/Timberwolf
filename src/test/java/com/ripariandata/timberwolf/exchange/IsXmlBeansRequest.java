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
package com.ripariandata.timberwolf.exchange;

import com.microsoft.schemas.exchange.services.x2006.messages.BaseRequestType;
import org.mockito.ArgumentMatcher;

import static org.mockito.Mockito.argThat;

/**
 * An ArgumentMatcher for XmlBeans generated xml objects as arguments to mocked
 * methods, because those don't generate an equals() method.
 *
 * @param <T> The specific Type of BaseRequestType.
 */
public class IsXmlBeansRequest<T extends BaseRequestType> extends ArgumentMatcher<T>
{
    private final T expected;

    /**
     * Creates a new XmlBeans argument matcher.
     * @param expected the expected xml block
     */
    public IsXmlBeansRequest(final T expectedType)
    {
        this.expected = expectedType;
    }

    /** A Helper method to make the when calls cleaner. */
    public static <T extends BaseRequestType> T likeThis(final T expected)
    {
        return argThat(new IsXmlBeansRequest<T>(expected));
    }


    @Override
    public boolean matches(final Object o)
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
