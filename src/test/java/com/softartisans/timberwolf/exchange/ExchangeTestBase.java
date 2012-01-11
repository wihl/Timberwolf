package com.softartisans.timberwolf.exchange;

import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Created by IntelliJ IDEA.
 * User: scottd
 * Date: 1/11/12
 * Time: 4:11 PM
 */
public class ExchangeTestBase
{
    @Mock
    public ExchangeService service;

    @Before
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
    }
}
