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

import org.eclipse.core.filesystem.IFileInfo;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.nio.NIOFileBasicAttribute;

import java.nio.file.attribute.FileTime;

public class NIOEFSBasicFileAttribute extends NIOFileBasicAttribute {

    private final IFileInfo fileInfo;

    public NIOEFSBasicFileAttribute(@NotNull IFileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    @Override
    @NotNull
    public FileTime lastModifiedTime() {
        return FileTime.fromMillis(fileInfo.getLastModified());
    }

    @Override
    public boolean isDirectory() {
        return fileInfo.isDirectory();
    }

    @Override
    public long size() {
        return fileInfo.getLength();
    }
}
