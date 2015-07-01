/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.*;
import java.util.Arrays;

/**
 * Memory content storage
 */
public class BytesContentStorage implements DBDContentStorage {

    static final Log log = Log.getLog(BytesContentStorage.class);

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
        return GeneralUtils.getDefaultFileEncoding();
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
        byte[] data = new byte[(int)contentLength];
        int count = stream.read(data);
        if (count >= 0 && count != contentLength) {
            log.warn("Actual content length (" + count + ") is less than declared: " + contentLength);
            data = Arrays.copyOf(data, count);
        }
        return new BytesContentStorage(data, encoding);
    }
}
