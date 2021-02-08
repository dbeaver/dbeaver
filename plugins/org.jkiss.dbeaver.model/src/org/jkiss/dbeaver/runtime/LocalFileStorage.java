/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.runtime;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.*;

/**
 * LocalFileStorage
 */
public class LocalFileStorage implements IStorage, IPersistentStorage, IEncodedStorage {

    private final File file;
    private final String charset;

    public LocalFileStorage(File file, String charset) {
        this.file = file;
        this.charset = charset;
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

    @Override
    public String getCharset() throws CoreException {
        return charset;
    }
}