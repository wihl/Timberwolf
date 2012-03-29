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
package com.ripariandata.timberwolf.writer.hbase;

import org.apache.hadoop.conf.Configuration;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for the HBaseConfigurator.
 */
public class HBaseConfiguratorTest
{
    /**
     * Simple test to actually make a non-default Hadoop configuration.
     */
    @Test
    public void testCreate()
    {
        String quorum = "someserver.somewhere.test";
        String clientPort = "2181";
        Configuration configuration = HBaseConfigurator.createConfiguration(quorum, clientPort);
        Assert.assertEquals(quorum, configuration.get("hbase.zookeeper.quorum"));
        Assert.assertEquals(clientPort, configuration.get("hbase.zookeeper.property.clientPort"));
    }
}
