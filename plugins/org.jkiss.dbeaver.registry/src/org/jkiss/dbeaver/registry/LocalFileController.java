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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBFileController;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalFileController implements DBFileController {

    protected final Path dataFolder;

    public LocalFileController(Path dataFolder) {
        this.dataFolder = dataFolder;
    }

    @Override
    public byte[] loadFileData(@NotNull String fileType, @NotNull String filePath) throws DBException {
        Path targetPath = getTargetPath(fileType, filePath);
        return getBytes(targetPath);
    }

    @Override
    public void saveFileData(@NotNull String fileType, @NotNull String filePath, byte[] fileData) throws DBException {
        Path targetPath = getTargetPath(fileType, filePath);
        try {
            if (!Files.exists(targetPath.getParent())) {
                Files.createDirectories(targetPath.getParent());
            }
            Files.write(targetPath, fileData);
        } catch (IOException e) {
            throw new DBException("Error writing file '" + targetPath.toAbsolutePath() + "' data: " + e.getMessage(), e);
        }
    }

    @Override
    public String[] listFiles(@NotNull String fileType, @NotNull String filePath) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void deleteFile(@NotNull String fileType, @NotNull String filePath, boolean recursive) throws DBException {
        Path targetPath = getTargetPath(fileType, filePath);
        try {
            Files.delete(targetPath);
        } catch (IOException e) {
            throw new DBException("Error deleting file '" + targetPath.toAbsolutePath() + "' data: " + e.getMessage(), e);
        }
    }

    @NotNull
    protected byte[] getBytes(Path targetPath) throws DBException {
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new DBException("Error reading file '" + targetPath.toAbsolutePath() + "' data", e);
        }
    }

    @NotNull
    protected Path getTargetPath(@NotNull String fileType, @NotNull String filePath) throws DBException {
        return dataFolder.resolve(fileType).resolve(filePath);
    }
}
