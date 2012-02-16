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

import com.ripariandata.timberwolf.MailboxItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;

public class MaildirEmail implements MailboxItem
{
    private static Map<String, String> maildirkeys = new HashMap<String, String>();
    private Map<String, String> headers = new HashMap<String, String>();
    private boolean skipThisEmail = false;

    static
    {
        maildirkeys.put("Subject", "Subject");
        maildirkeys.put("Date", "Time Sent");
        maildirkeys.put("Message-ID", "Item ID");
        maildirkeys.put("From", "Sender");
        maildirkeys.put("To", "To");
        maildirkeys.put("Cc", "Cc");
        maildirkeys.put("Bcc", "Bcc");
    }

    private String readToEnd(BufferedReader reader) throws IOException
    {
        String s = reader.readLine();
        while (true)
        {
            String line = reader.readLine();
            if (line == null)
            {
                break;
            }

            if (isMultipartHeader(line))
            {
                skipThisEmail = true;
                return s;
            }

            s += "\n";
            s += line;
        }
        return s;
    }

    private static boolean isMultipartHeader(String line)
    {
        String[] parts = line.split(":", 2);
        return parts[0].equals("Content-type") && parts[1].trim().startsWith("multipart");
    }

    public MaildirEmail(File file) throws IOException, FileNotFoundException
    {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = reader.readLine();
        while (true)
        {
            if (line == null || line.trim().equals(""))
            {
                break;
            }

            if (isMultipartHeader(line))
            {
                skipThisEmail = true;
                return;
            }

            String[] parts = line.split(":", 2);
            if (maildirkeys.containsKey(parts[0]))
            {
                String header = parts[1] != null ? parts[1].trim() : "";
                while (true)
                {
                    line = reader.readLine();
                    if (line == null || !(line.startsWith(" ") || line.startsWith("\t")))
                    {
                        break;
                    }
                    else
                    {
                        header += line != null ? (" " + line.trim()) : "";
                    }
                }

                headers.put(maildirkeys.get(parts[0]), header);
            }
            else
            {
                line = reader.readLine();
            }
        }

        headers.put("Body", readToEnd(reader));
    }

    public boolean skip()
    {
        return skipThisEmail;
    }

    @Override
    public String[] getHeaderKeys()
    {
        return headers.keySet().toArray(new String[headers.keySet().size()]);
    }

    @Override
    public String[] possibleHeaderKeys()
    {
        String[] possibles = new String[maildirkeys.size() + 1];
        int i = 1;
        for (String key : maildirkeys.values())
        {
            possibles[i] = key;
            i++;
        }
        possibles[0] = "Body";
        return possibles;
    }

    @Override
    public boolean hasKey(String key)
    {
        return headers.containsKey(key);
    }

    @Override
    public String getHeader(String key)
    {
        return headers.get(key);
    }
}