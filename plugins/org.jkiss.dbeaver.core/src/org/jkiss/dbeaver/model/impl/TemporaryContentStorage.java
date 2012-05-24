/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.model.data.DBDContentStorageLocal;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * File content storage
 */
public class TemporaryContentStorage implements DBDContentStorageLocal {

    static final Log log = LogFactory.getLog(TemporaryContentStorage.class);

    private IFile file;

    public TemporaryContentStorage(IFile file)
    {
        this.file = file;
    }

    @Override
    public InputStream getContentStream()
        throws IOException
    {
        try {
            return file.getContents();
        }
        catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public Reader getContentReader()
        throws IOException
    {
        try {
            return new InputStreamReader(
                file.getContents(),
                file.getCharset());
        }
        catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long getContentLength()
    {
        return file.getLocation().toFile().length();
    }

    @Override
    public String getCharset()
    {
        try {
            return file.getCharset();
        }
        catch (CoreException e) {
            log.warn(e);
            return ContentUtils.DEFAULT_FILE_CHARSET;
        }
    }

    @Override
    public DBDContentStorage cloneStorage(DBRProgressMonitor monitor)
        throws IOException
    {
        // Create new local storage
        IFile tempFile = ContentUtils.createTempContentFile(monitor, "copy" + this.hashCode());
        try {
            InputStream is = file.getContents(true);
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

    @Override
    public void release()
    {
        try {
            file.delete(true, false, new NullProgressMonitor());
        }
        catch (CoreException e) {
            log.warn(e);
        }
    }

    @Override
    public IFile getDataFile()
    {
        return file;
    }
}
