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
package com.ripariandata.timberwolf.maildir;

import com.ripariandata.timberwolf.MailStore;
import com.ripariandata.timberwolf.MailboxItem;
import com.ripariandata.timberwolf.UserFolderSyncStateStorage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

public class MaildirMailStore implements MailStore
{
    private String directory;

    public MaildirMailStore(String dir)
    {
        directory = dir;
    }

    @Override
    public Iterable<MailboxItem> getMail(Iterable<String> targetUsers, UserFolderSyncStateStorage syncStateStorage)
    {
        return new Iterable<MailboxItem>()
        {
            @Override
            public Iterator<MailboxItem> iterator()
            {
                return new MaildirEmailIterator(new FileIterator(directory));
            }
        };
    }

    private class MaildirEmailIterator implements Iterator<MailboxItem>
    {
        private Iterator<File> files;

        public MaildirEmailIterator(Iterator<File> fs)
        {
            files = fs;
        }

        @Override
        public boolean hasNext()
        {
            return files.hasNext();
        }

        @Override
        public MailboxItem next()
        {
            try
            {
                return new MaildirEmail(files.next());
            }
            catch (FileNotFoundException e)
            {
                System.err.println(e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Bad things happened.");
            }
            catch (IOException e)
            {
                System.err.println(e.getMessage());
                e.printStackTrace();
                throw new RuntimeException("Bad things happened.");
            }
        }

        @Override
        public void remove()
        {
            files.remove();
        }
    }

    /**
     * Returns all the files in all the directories (and so on) underneath the given
     * directory.
     */
    private class FileIterator implements Iterator<File>
    {
        Queue<String> directories = new LinkedList<String>();
        Queue<String> files = new LinkedList<String>();

        public FileIterator(String dir)
        {
            directories.add(dir);
        }

        private void fillCollections()
        {
            String nextDir = directories.remove();

            for (File entry : new File(nextDir).listFiles())
            {
                if (entry.isFile())
                {
                    files.add(entry.getAbsolutePath());
                }
                else if (entry.isDirectory())
                {
                    directories.add(entry.getAbsolutePath());
                }
            }

            if (files.size() == 0 && directories.size() > 0)
            {
                fillCollections();
            }
        }

        @Override
        public boolean hasNext()
        {
            if (directories.size() == 0)
            {
                if (files.size() == 0)
                {
                    return false;
                }

                return true;
            }

            if (files.size() > 0)
            {
                return true;
            }

            fillCollections();

            return files.size() > 0;
        }

        @Override
        public File next()
        {
            if (hasNext())
            {
                return new File(files.remove());
            }

            return null;
        }

        @Override
        public void remove()
        {
            throw new UnsupportedOperationException();
        }
    }
}
