/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.fs.efs;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.nio.ByteArrayChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.util.Set;


public class NIOEFSByteArrayChannel extends ByteArrayChannel {

    private final IFileStore store;

    public NIOEFSByteArrayChannel(@NotNull byte[] buf, @NotNull Set<? extends OpenOption> options, @NotNull IFileStore store) {
        super(buf, options);
        this.store = store;
    }

    @Override
    protected void createNewFile() throws IOException {
        try {
            IFileInfo info = store.fetchInfo(EFS.NONE, null);
            if (info.exists()) {
                throw new FileAlreadyExistsException(store.toURI().toString());
            }
            // creating a zero-length file: open/close an output stream
            try (OutputStream out = store.openOutputStream(EFS.NONE, null)) {
            } catch (CoreException e) {
                throw new IOException(e);
            }
        } catch (CoreException e) {
            throw new IOException(e);
        }

    }

    @Override
    protected void writeToFile() throws IOException {
        try (OutputStream out = store.openOutputStream(EFS.NONE, null)) {
            out.write(buf);
            out.flush();
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected void deleteFile() throws IOException {
        try {
            store.delete(EFS.NONE, null);
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }
}
