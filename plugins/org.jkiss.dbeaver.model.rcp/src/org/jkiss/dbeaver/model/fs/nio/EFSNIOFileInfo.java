/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.fs.nio;

import org.eclipse.core.filesystem.provider.FileInfo;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * NIOFileInfo
 */
public class EFSNIOFileInfo extends FileInfo {

    private final Path path;

    public EFSNIOFileInfo(Path path) {
        super(EFSNIOResource.getPathFileNameOrHost(path));
        this.path = path;
    }

    @Override
    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public long getLength() {
        try {
            return Files.size(path);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public boolean isDirectory() {
        return Files.isDirectory(path);
    }

    @Override
    public boolean exists() {
        return true;
    }
}
