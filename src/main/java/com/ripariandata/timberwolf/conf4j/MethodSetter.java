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
package com.ripariandata.timberwolf.conf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class MethodSetter implements Setter
{
    private Object target;
    private Method method;

    public MethodSetter(Object o, Method m)
    {
        target = o;
        method = m;
    }

    @Override
    public void set(Object value) throws ConfigFileException
    {
        try
        {
            try
            {
                method.invoke(target, value);
            }
            catch (IllegalAccessException e)
            {
                method.setAccessible(true);
                try
                {
                    method.invoke(target, value);
                }
                catch (IllegalAccessException iae)
                {
                    throw new IllegalAccessError(iae.getMessage());
                }
            }
        }
        catch (InvocationTargetException e)
        {
            Throwable t = e.getCause();
            if (t instanceof RuntimeException)
            {
                throw (RuntimeException)t;
            }
            if (t instanceof Error)
            {
                throw (Error)t;
            }
            if (t instanceof ConfigFileException)
            {
                throw (ConfigFileException)t;
            }

            throw new ConfigFileException(t == null ? e : t);
        }
    }
}
