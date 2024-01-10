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

import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;

import java.nio.file.Path;

/**
 * DataSourceStorage
 */
class DataSourceFileStorage implements DBPDataSourceConfigurationStorage
{
    private Path sourceFile;
    private boolean isLegacy;
    private boolean isDefault;
    private String configSuffix;

    DataSourceFileStorage(Path sourceFile, boolean isLegacy, boolean isDefault) {
        this.sourceFile = sourceFile;
        this.isLegacy = isLegacy;
        this.isDefault = isDefault;

        if (isDefault) {
            configSuffix = "";
        } else {
            String configFileName = sourceFile.getFileName().toString();
            configSuffix = configFileName.substring(
                DBPDataSourceRegistry.MODERN_CONFIG_FILE_PREFIX.length(),
                configFileName.length() - DBPDataSourceRegistry.MODERN_CONFIG_FILE_EXT.length());
        }

    }

    @Override
    public String getStorageId() {
        return "file://" + sourceFile.toAbsolutePath();
    }

    @Override
    public String getStorageName() {
        return sourceFile.getFileName().toString();
    }

    public boolean isLegacy() {
        return isLegacy;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public String getStatus() {
        return "Valid";
    }

    public String getName() {
        return sourceFile.getFileName().toString();
    }

    public String getStorageSubId() {
        return configSuffix;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    void convertToModern(DBPProject project) {
        this.sourceFile = project.getMetadataFolder(true).resolve(DBPDataSourceRegistry.MODERN_CONFIG_FILE_NAME);
        this.isLegacy = false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof DataSourceFileStorage &&
            sourceFile.equals(((DataSourceFileStorage) obj).sourceFile);
    }

    @Override
    public int hashCode() {
        return sourceFile.hashCode();
    }

    @Override
    public String toString() {
        return sourceFile.toAbsolutePath().toString();
    }
}
