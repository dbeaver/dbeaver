/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.dbeaver.model.connection.DBPDriverLibrary;

import java.nio.file.Path;

public class DriverFileInfo {
    private final String id;
    private final String version;
    private final DBPDriverLibrary.FileType type;
    private final Path file;
    private final String fileLocation;
    private long fileCRC;

    public DriverFileInfo(String id, String version, DBPDriverLibrary.FileType type, Path file, String fileLocation) {
        this.id = id;
        this.version = version;
        this.file = file;
        this.type = type;
        this.fileLocation = fileLocation;
    }

    DriverFileInfo(DBPDriverLibrary library) {
        this.id = library.getId();
        this.version = library.getVersion();
        this.file = library.getLocalFile();
        this.type = library.getType();
        this.fileLocation = library.getLocalFile() != null ? library.getLocalFile().toString() : library.getPath();
        this.fileCRC = library.getFileCRC();
    }

    public Path getFile() {
        return file;
    }

    public String getFileLocation() {
        return fileLocation;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public DBPDriverLibrary.FileType getType() {
        return type;
    }

    public long getFileCRC() {
        return fileCRC;
    }

    public void setFileCRC(long fileCRC) {
        this.fileCRC = fileCRC;
    }

    @Override
    public String toString() {
        return file != null ? file.getFileName().toString() : this.id;
    }
}
