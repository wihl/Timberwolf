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
package com.ripariandata.timberwolf.hive;

import com.ripariandata.timberwolf.MailWriter;
import com.ripariandata.timberwolf.MailboxItem;

public class HiveMailWriter extends MailWriter
{
    public void write(Iterable<MailboxItem> mail)
    {
        // TODO: Open Hive JDBC connection, check that target table is available with `show tables`.
        // TODO: If it is, excellent.
        // TODO: If not, create it.
        // TODO: Open HDFS connection (with `FileSystem.get`) to timberwolf's temporary folder.
        // TODO: Write sequence file into that temp folder.
        // TODO: Use Hive JDBC connection to use `load data` on the file we just wrote into the
        //       table from before.
        // TODO: Dispose of everything.
    }
}
