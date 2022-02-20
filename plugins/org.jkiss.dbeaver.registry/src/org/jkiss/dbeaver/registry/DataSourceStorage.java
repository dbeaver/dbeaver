/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * DataSourceStorage
 */
class DataSourceStorage implements DBPDataSourceConfigurationStorage
{
    private final Path sourceFile;
    private final boolean isDefault;
    private final String configSuffix;

    DataSourceStorage(Path sourceFile, boolean isDefault) {
        this.sourceFile = sourceFile;
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

    public String getConfigurationFileSuffix() {
        return configSuffix;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public Path getSourceFile() {
        return sourceFile;
    }

    @Override
    public List<? extends DBPDataSourceContainer> loadDataSources(DBPDataSourceRegistry registry, Map<String, Object> options) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public String toString() {
        return sourceFile.toAbsolutePath().toString();
    }
}
