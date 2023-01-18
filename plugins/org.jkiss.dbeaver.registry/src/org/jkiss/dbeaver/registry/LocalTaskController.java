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
package org.jkiss.dbeaver.registry;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.task.DBTTaskController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalTaskController implements DBTTaskController {

    public LocalTaskController() {
    }

    @Override
    public String loadTaskConfigurationFile(@NotNull String projectId, @NotNull String filePath) throws DBException {
        Path localPath = getMetadataFolder(projectId, false).resolve(filePath);
        if (Files.notExists(localPath)) {
            return null;
        }
        try {
            return Files.readString(localPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DBException("Error reading task configuration file '" + filePath + "'", e);
        }
    }

    @Override
    public void saveTaskConfigurationFile(
        @NotNull String projectId,
        @NotNull String filePath,
        @Nullable String data
    ) throws DBException {
        Path localPath = getMetadataFolder(projectId, true).resolve(filePath);
        if (Files.exists(localPath)) {
            ContentUtils.makeFileBackup(localPath);
        }
        if (data == null && Files.exists(localPath)) {
            try {
                Files.delete(localPath);
            } catch (IOException e) {
                throw new DBException("Error deleting task configuration file '" + filePath + "'", e);
            }
            return;
        }
        if (data == null) {
            throw new DBException("Error saving task configuration file '" + filePath + "'");
        }
        try {
            Files.writeString(localPath, data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DBException("Error writing task configuration file '" + filePath + "'", e);
        }
    }

    /**
     * Return path to metadata folder with tasks file inside
     *
     * @param projectId unique project name
     * @param create will create metadata folder if true
     * @return path to metadata folder
     * @throws DBException on case of folder creation exception
     */
    @NotNull
    public Path getMetadataFolder(@NotNull String projectId, boolean create) throws DBException {
        Path parent = DBWorkbench.getPlatform().getWorkspace().getProject(projectId).getMetadataFolder(create);
        createFolder(create, parent);
        return parent;
    }

    protected void createFolder(boolean create, Path metadataFolder) throws DBException {
        if (create && Files.notExists(metadataFolder)) {
            try {
                Files.createDirectories(metadataFolder);
            } catch (IOException e) {
                throw new DBException("Error creating metadata folder", e);
            }
        }
    }
}
