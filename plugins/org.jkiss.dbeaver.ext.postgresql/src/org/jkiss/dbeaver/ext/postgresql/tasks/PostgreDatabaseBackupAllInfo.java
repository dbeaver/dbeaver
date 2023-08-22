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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;

import java.util.List;

public class PostgreDatabaseBackupAllInfo {

    @NotNull
    private PostgreDataSource dataSource;
    private List<PostgreDatabase> databases;

    public PostgreDatabaseBackupAllInfo(@NotNull PostgreDataSource dataSource, List<PostgreDatabase> databases) {
        this.dataSource = dataSource;
        this.databases = databases;
    }

    @NotNull
    public PostgreDataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(@NotNull PostgreDataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<PostgreDatabase> getDatabases() {
        return databases;
    }

    public void setDatabases(List<PostgreDatabase> databases) {
        this.databases = databases;
    }
}
