/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;

/**
 * String content storage
 */
public class StringContentStorage implements DBDContentStorage, DBDContentCached {

    private static final Log log = Log.getLog(StringContentStorage.class);

    private String data;

    public StringContentStorage(String data)
    {
        this.data = data;
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        return new ByteArrayInputStream(data.getBytes(GeneralUtils.getDefaultFileEncoding()));
    }

    @Override
    public Reader getContentReader()
        throws IOException
    {
        return new StringReader(CommonUtils.notEmpty(data));
    }

    @Override
    public long getContentLength()
    {
        return data == null ? 0 : data.length();
    }

    @Override
    public String getCharset()
    {
        return GeneralUtils.getDefaultFileEncoding();
    }

    @Override
    public DBDContentStorage cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        return new StringContentStorage(data);
    }

    @Override
    public void release()
    {
        data = null;
    }

    public static StringContentStorage createFromReader(
        Reader stream,
        long contentLength)
        throws IOException
    {
        if (contentLength > Integer.MAX_VALUE / 2) {
            throw new IOException("Too big content length for memory storage: " + contentLength);
        }
        StringBuilder buffer = new StringBuilder((int) contentLength);
        char[] charBuffer = new char[10000];
        for (;;) {
            int count = stream.read(charBuffer);
            if (count <= 0) {
                break;
            }
            buffer.append(charBuffer, 0, count);
        }
        if (buffer.length() != contentLength) {
            log.warn("Actual content length (" + buffer.length() + ") is less than declared: " + contentLength);
        }
        return new StringContentStorage(buffer.toString());
    }

    @NotNull
    public static StringContentStorage createFromReader(Reader stream)
        throws IOException
    {
        StringBuilder buffer = new StringBuilder(1000);
        for (;;) {
            char[] charBuffer = new char[10000];
            int count = stream.read(charBuffer);
            if (count <= 0) {
                break;
            }
            buffer.append(charBuffer, 0, count);
        }
        return new StringContentStorage(buffer.toString());
    }

    @Override
    public String getCachedValue() {
        return data;
    }
}
