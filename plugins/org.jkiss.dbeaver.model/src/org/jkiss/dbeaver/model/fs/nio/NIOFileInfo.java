/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class NIOFileInfo implements IFileInfo {
    private static final Log log = Log.getLog(NIOFileInfo.class);

    private final Path path;
    private volatile Boolean directory;
    private boolean exists;

    public NIOFileInfo(@NotNull Path path) {
        this.path = path;
        this.exists = true;
    }

    @Override
    public boolean exists() {
        return exists;
    }

    public void setExists(boolean exists) {
        this.exists = exists;
    }

    public void setDirectory(boolean directory) {
        this.directory = directory;
    }

    @Override
    public int getError() {
        return EFS.NONE;
    }

    @Override
    public boolean getAttribute(int attribute) {
        if (true) {
            return false;
        }

        try {
            switch (attribute) {
                case EFS.ATTRIBUTE_READ_ONLY:
                    return !Files.isWritable(path);
                case EFS.ATTRIBUTE_EXECUTABLE:
                    return Files.isExecutable(path);
                case EFS.ATTRIBUTE_ARCHIVE:
                    return false;
                case EFS.ATTRIBUTE_HIDDEN:
                    return Files.isHidden(path);
                case EFS.ATTRIBUTE_SYMLINK:
                    return Files.isSymbolicLink(path);
                default:
                    log.debug("Requested unsupported attribute: " + attribute);
                    break;
            }
        } catch (IOException e) {
            log.debug("Error retrieving attribute " + attribute + ": " + e.getMessage());
        }

        return false;
    }

    @Override
    public String getStringAttribute(int attribute) {
        try {
            switch (attribute) {
                case EFS.ATTRIBUTE_LINK_TARGET:
                    return Files.readSymbolicLink(path).toString();
                default:
                    log.debug("Requested unsupported string attribute: " + attribute);
                    break;
            }
        } catch (IOException e) {
            log.debug("Error retrieving string attribute " + attribute + ": " + e.getMessage());
        }

        return null;
    }

    @Override
    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            log.debug("Error retrieving last modified time: " + e.getMessage());
            return EFS.NONE;
        }
    }

    @Override
    public long getLength() {
        try {
            return Files.size(path);
        } catch (IOException e) {
            log.debug("Error retrieving file size: " + e.getMessage());
            return EFS.NONE;
        }
    }

    @Override
    public String getName() {
        return path.getFileName().toString();
    }

    @Override
    public boolean isDirectory() {
        if (directory == null) {
            synchronized (this) {
                if (directory == null) {
                    directory = Files.isDirectory(path);
                }
            }
        }

        return directory;
    }

    @Override
    public void setAttribute(int attribute, boolean value) {
        // not implemented
    }

    @Override
    public void setLastModified(long time) {
        // not implemented
    }

    @Override
    public int compareTo(IFileInfo o) {
        return path.compareTo(((NIOFileInfo) o).path);
    }
}
