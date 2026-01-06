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
import org.jkiss.dbeaver.model.DBConfigurationController;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Local configuration controller. Keeps files on hard drive.
 */
public class LocalConfigurationController implements DBConfigurationController {

    protected final Path configFolder;
    private Path legacyConfigFolder;

    public LocalConfigurationController(Path configFolder) {
        this.configFolder = configFolder;
    }

    public void setLegacyConfigFolder(Path legacyConfigFolder) {
        this.legacyConfigFolder = legacyConfigFolder;
    }

    @Override
    public String loadConfigurationFile(@NotNull String filePath) throws DBException {
        Path localPath = configFolder.resolve(filePath);
        if (!localPath.normalize().startsWith(configFolder)) {
            throw new DBException("Invalid configuration path");
        }

        if (!Files.exists(localPath)) {
            // Try to get it from legacy location
            if (legacyConfigFolder != null) {
                localPath = legacyConfigFolder.resolve(filePath);
            }
            if (!Files.exists(localPath)) {
                return null;
            }
        }
        try {
            return Files.readString(localPath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DBException("Error reading configuration file '" + filePath + "'", e);
        }
    }

    @Override
    public void saveConfigurationFile(@NotNull String filePath, @NotNull String data) throws DBException {
        Path localPath = configFolder.resolve(filePath);
        if (!localPath.normalize().startsWith(configFolder)) {
            throw new DBException("Invalid configuration path '" + localPath + "'");
        }
        try {
            Path localFolder = localPath.getParent();
            if (!Files.exists(localFolder)) {
                Files.createDirectories(localFolder);
            }
            if (Files.exists(localPath)) {
                ContentUtils.makeFileBackup(localPath);
            }
            Files.writeString(localPath, data, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DBException("Error writing configuration file '" + filePath + "'", e);
        }
    }
}
