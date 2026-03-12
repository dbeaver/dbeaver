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
import org.jkiss.dbeaver.DBException;

import java.nio.file.Path;
import java.util.List;

/**
 * Interface for handling the import of files into a database or other target systems.
 * Implementations of this interface are responsible for processing a list of file paths
 */
public interface FileImportHandler {
    /**
     * Imports the specified files into the system using the provided processor type.
     * This method processes a list of file paths and uses the given processor type
     * to handle the import operation.
     *
     * @param filePath A list of {@code Path} objects representing the file paths to be imported.
     *                 The list must not be null and should contain valid file paths.
     * @param processorType A {@code String} specifying the type of processor to be used
     *                      for handling the import. The string must not be null.
     */
    void importFiles(@NotNull List<Path> filePath, @NotNull String processorType) throws DBException;
}
