/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;
import java.util.Arrays;

/**
 * String content storage
 */
public class StringContentStorage implements DBDContentStorage {

    static final Log log = LogFactory.getLog(StringContentStorage.class);

    private String data;

    public StringContentStorage(String data)
    {
        this.data = data;
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        return new ByteArrayInputStream(data.getBytes());
    }

    @Override
    public Reader getContentReader()
        throws IOException
    {
        return new StringReader(data);
    }

    @Override
    public long getContentLength()
    {
        return data.length();
    }

    @Override
    public String getCharset()
    {
        return ContentUtils.DEFAULT_FILE_CHARSET;
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
        char[] data = new char[(int)contentLength];
        int count = stream.read(data);
        if (count >= 0 && count != contentLength) {
            log.warn("Actual content length (" + count + ") is less than declared: " + contentLength);
            data = Arrays.copyOf(data, count);
        }
        return new StringContentStorage(String.valueOf(data));
    }

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
}
