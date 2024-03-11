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
package org.jkiss.dbeaver.model.dashboard;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;

/**
 * Dashboard context
 */
public class DBDashboardContext {

    @Nullable
    private DBPProject project;

    @Nullable
    private DBPDataSourceContainer dataSource;

    public DBDashboardContext() {
    }

    public DBDashboardContext(@Nullable DBPProject project) {
        this.project = project;
    }

    public DBDashboardContext(@Nullable DBPDataSourceContainer dataSource) {
        this.project = dataSource == null ? null : dataSource.getProject();
        this.dataSource = dataSource;
    }

    @Nullable
    public DBPProject getProject() {
        return project;
    }

    public void setProject(@Nullable DBPProject project) {
        this.project = project;
    }

    @Nullable
    public DBPDataSourceContainer getDataSource() {
        return dataSource;
    }

    public void setDataSource(@Nullable DBPDataSourceContainer dataSource) {
        this.dataSource = dataSource;
    }
}
