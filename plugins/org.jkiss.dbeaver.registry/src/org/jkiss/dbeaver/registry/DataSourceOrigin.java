/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.resources.IFile;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;

import java.util.List;
import java.util.Map;

/**
 * DataSourceOrigin
 */
class DataSourceOrigin implements DBPDataSourceConfigurationStorage
{
    private final IFile sourceFile;
    private final boolean isDefault;
    private final String configSuffix;

    DataSourceOrigin(IFile sourceFile, boolean isDefault) {
        this.sourceFile = sourceFile;
        this.isDefault = isDefault;

        if (isDefault) {
            configSuffix = "";
        } else {
            String configFileName = sourceFile.getName();
            configSuffix = configFileName.substring(
                DBPDataSourceRegistry.MODERN_CONFIG_FILE_PREFIX.length(),
                configFileName.length() - DBPDataSourceRegistry.MODERN_CONFIG_FILE_EXT.length());
        }

    }

    @Override
    public String getStorageId() {
        return "file://" + sourceFile.getFullPath().toString();
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
        return sourceFile.getName();
    }

    public String getConfigurationFileSuffix() {
        return configSuffix;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    public IFile getSourceFile() {
        return sourceFile;
    }

    @Override
    public List<? extends DBPDataSourceContainer> loadDataSources(DBPDataSourceRegistry registry, Map<String, Object> options) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public String toString() {
        return sourceFile.getFullPath().toString();
    }
}
