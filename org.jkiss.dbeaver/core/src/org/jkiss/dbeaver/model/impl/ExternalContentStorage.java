/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.InputStreamReader;

/**
 * File content storage
 */
public class ExternalContentStorage implements DBDContentStorage {

    static Log log = LogFactory.getLog(ExternalContentStorage.class);

    private File file;
    private String charset;

    public ExternalContentStorage(File file)
    {
        this(file, ContentUtils.DEFAULT_FILE_CHARSET);
    }

    public ExternalContentStorage(File file, String charset)
    {
        this.file = file;
        this.charset = charset;
    }

    public InputStream getContentStream()
        throws IOException
    {
        return new FileInputStream(file);
    }

    public Reader getContentReader()
        throws IOException
    {
        return new InputStreamReader(getContentStream(), charset);
    }

    public long getContentLength()
    {
        return file.length();
    }

    public String getCharset()
    {
        return charset;
    }

    public void release()
    {
        // Do nothing
/*
        if (!file.delete()) {
            log.warn("Could not delete temporary file");
        }
*/
    }
}