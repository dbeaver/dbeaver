/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
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
 * Memory content storage
 */
public class BytesContentStorage implements DBDContentStorage {

    static final Log log = LogFactory.getLog(BytesContentStorage.class);

    private byte[] data;

    public BytesContentStorage(byte[] data)
    {
        this.data = data;
    }

    public InputStream getContentStream()
        throws IOException
    {
        return new ByteArrayInputStream(data);
    }

    public Reader getContentReader()
        throws IOException
    {
        return new InputStreamReader(
            getContentStream(),
            ContentUtils.DEFAULT_FILE_CHARSET);
    }

    public long getContentLength()
    {
        return data.length;
    }

    public String getCharset()
    {
        return ContentUtils.DEFAULT_FILE_CHARSET;
    }

    public DBDContentStorage cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        return new BytesContentStorage(data);
    }

    public void release()
    {
        data = null;
    }

    public static BytesContentStorage createFromStream(
        InputStream stream,
        long contentLength)
        throws IOException
    {
        if (contentLength > Integer.MAX_VALUE) {
            throw new IOException("Too big content length for memory storage: " + contentLength);
        }
        byte[] data = new byte[(int)contentLength];
        int count = stream.read(data);
        if (count != contentLength) {
            log.warn("Actual content length (" + count + ") is less than declared: " + contentLength);
            data = Arrays.copyOf(data, count);
        }
        return new BytesContentStorage(data);
    }
}
