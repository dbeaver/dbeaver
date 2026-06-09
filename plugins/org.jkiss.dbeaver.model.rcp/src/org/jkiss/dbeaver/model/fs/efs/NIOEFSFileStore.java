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
import org.jkiss.dbeaver.model.nio.NIOFileStore;

import java.io.IOException;

public class NIOEFSFileStore extends NIOFileStore {

    private final IFileStore efsFileStore;

    public NIOEFSFileStore(@NotNull IFileStore efsFileStore) {
        this.efsFileStore = efsFileStore;
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
