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
package org.jkiss.dbeaver.model.file;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for handling the import of files into a database or other target systems.
 * Implementations of this interface are responsible for processing a list of file paths
 * and optionally filtering by a specific file extension.
 */
public interface IFileImportHandler {
    /**
     * Imports a list of files, optionally filtered by a specified file extension, into a target system.
     *
     * @param filePath  the list of file paths to import; must not be null.
     * @param extension the file extension to filter the files by; can be null to include all files.
     * @throws DBException if an error occurs during the import operation.
     */
    void importFiles(@NotNull List<Path> filePath, @Nullable String extension) throws DBException;
}
