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

import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPProject;

/**
 * DataSourceRemoteStorage
 */
class DataSourceRemoteStorage implements DBPDataSourceConfigurationStorage {

    private DBPProject project;

    public DataSourceRemoteStorage(DBPProject project) {
        this.project = project;
    }

    @Override
    public String getStorageId() {
        return project.getName();
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isDefault() {
        return true;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @Override
    public String getStatus() {
        return null;
    }

    @Override
    public String getStorageSubId() {
        return null;
    }
}
