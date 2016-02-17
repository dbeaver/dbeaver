/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.runtime;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.editors.IPersistentStorage;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.*;

/**
 * LocalFileStorage
 */
public class LocalFileStorage implements IStorage, IPersistentStorage {

    private final File file;

    public LocalFileStorage(File file) {
        this.file = file;
    }

    @Override
    public InputStream getContents() throws CoreException {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    @Override
    public IPath getFullPath() {
        return new Path(file.getAbsolutePath());
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public boolean isReadOnly() {
        return !file.canWrite();
    }

    @Override
    public void setContents(IProgressMonitor monitor, InputStream stream) throws CoreException {
        try (OutputStream os = new FileOutputStream(file)) {
            ContentUtils.copyStreams(stream, 0, os, RuntimeUtils.makeMonitor(monitor));
        } catch (IOException e) {
            throw new CoreException(GeneralUtils.makeExceptionStatus(e));
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

}