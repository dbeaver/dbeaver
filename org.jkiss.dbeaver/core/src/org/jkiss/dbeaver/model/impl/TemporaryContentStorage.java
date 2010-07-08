/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl;

import org.jkiss.dbeaver.model.data.DBDContentStorage;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.InputStream;
import java.io.IOException;

/**
 * File content storage
 */
public class TemporaryContentStorage implements DBDContentStorage {

    static Log log = LogFactory.getLog(TemporaryContentStorage.class);

    private IFile file;

    public TemporaryContentStorage(IFile file)
    {
        this.file = file;
    }

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

    public long getContentLength()
    {
        return file.getLocation().toFile().length();
    }

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

    public void release()
    {
        try {
            file.delete(true, false, new NullProgressMonitor());
        }
        catch (CoreException e) {
            log.warn(e);
        }
    }
}
