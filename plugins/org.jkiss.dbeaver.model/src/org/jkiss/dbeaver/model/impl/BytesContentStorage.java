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

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContentCached;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.IOUtils;

import java.io.*;

/**
 * Memory content storage
 */
public class BytesContentStorage implements DBDContentStorage, DBDContentCached {

    private static final Log log = Log.getLog(BytesContentStorage.class);

    private byte[] data;
    private String encoding;

    public BytesContentStorage(byte[] data, String encoding)
    {
        this.data = data;
        this.encoding = encoding;
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        return new ByteArrayInputStream(data);
    }

    @Override
    public Reader getContentReader()
        throws IOException
    {
        return new InputStreamReader(
            getContentStream(),
            encoding);
    }

    @Override
    public long getContentLength()
    {
        return data.length;
    }

    @Override
    public String getCharset()
    {
        return encoding;
    }

    @Override
    public DBDContentStorage cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        return new BytesContentStorage(data, encoding);
    }

    @Override
    public void release()
    {
        data = null;
    }

    public static BytesContentStorage createFromStream(
        InputStream stream,
        long contentLength,
        String encoding)
        throws IOException
    {
        if (contentLength > Integer.MAX_VALUE) {
            throw new IOException("Too big content length for memory storage: " + contentLength);
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtils.copyStream(stream, baos);
        byte[] result = baos.toByteArray();
        if (result.length != contentLength) {
            log.warn("Actual content length (" + result.length + ") is less than declared: " + contentLength);
        }
        return new BytesContentStorage(result, encoding);
    }

    @Override
    public Object getCachedValue() {
        return data;
    }
}
