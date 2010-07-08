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

/**
 * File content storage
 */
public class FileContentStorage2 implements DBDContentStorage {

    static Log log = LogFactory.getLog(FileContentStorage2.class);

    private File file;
    private String charset;

    public FileContentStorage2(File file)
    {
        this(file, ContentUtils.DEFAULT_FILE_CHARSET);
    }

    public FileContentStorage2(File file, String charset)
    {
        this.file = file;
        this.charset = charset;
    }

    public InputStream getContentStream()
        throws IOException
    {
        return new FileInputStream(file);
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