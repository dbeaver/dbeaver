/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.*;

/**
 * File content storage
 */
public class ExternalContentStorage implements DBDContentStorage {

    static final Log log = LogFactory.getLog(ExternalContentStorage.class);

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
        return new InputStreamReader(new FileInputStream(file), charset);
    }

    public long getContentLength()
    {
        return file.length();
    }

    public String getCharset()
    {
        return charset;
    }

    public DBDContentStorage cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        // Create new local storage
        IFile tempFile = ContentUtils.createTempContentFile(monitor, "copy" + this.hashCode());
        try {
            InputStream is = new FileInputStream(file);
            try {
                tempFile.setContents(is, true, false, monitor.getNestedMonitor());
            }
            finally {
                ContentUtils.close(is);
            }
        } catch (CoreException e) {
            ContentUtils.deleteTempFile(monitor, tempFile);
            throw new IOException(e);
        }
        return new TemporaryContentStorage(tempFile);
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