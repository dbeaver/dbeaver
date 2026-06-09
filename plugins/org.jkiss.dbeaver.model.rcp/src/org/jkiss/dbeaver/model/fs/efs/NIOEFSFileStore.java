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

import org.eclipse.core.filesystem.IFileStore;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileStore;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.attribute.FileStoreAttributeView;

public class NIOEFSFileStore extends FileStore {

    private final IFileStore efsFileStore;

    public NIOEFSFileStore(@NotNull IFileStore efsFileStore) {
        this.efsFileStore = efsFileStore;
    }

    @Override
    public long getUsableSpace() throws IOException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public long getUnallocatedSpace() throws IOException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public boolean supportsFileAttributeView(Class<? extends FileAttributeView> type) {
        return false;
    }

    @Override
    public boolean supportsFileAttributeView(String name) {
        return false;
    }

    @Override
    @Nullable
    public <V extends FileStoreAttributeView> V getFileStoreAttributeView(Class<V> type) {
        return null;
    }

    @Override
    public Object getAttribute(String attribute) throws IOException {
        throw new UnsupportedOperationException("Does not support the given attribute: " + attribute);
    }

    @Override
    @NotNull
    public String name() {
        return efsFileStore.getName();
    }

    @Override
    @NotNull
    public String type() {
        return efsFileStore.getFileSystem().getScheme();
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    @Override
    public long getTotalSpace() throws IOException {
        return 0;
    }
}
