/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;

import java.util.List;

/**
* PostgreDatabaseBackupInfo
*/
public class PostgreDatabaseBackupInfo extends PostgreDatabaseBackupRestoreInfo {
    @Nullable
    private List<PostgreSchema> schemas;
    @Nullable
    private List<PostgreTableBase> tables;

    public PostgreDatabaseBackupInfo(@NotNull PostgreDatabase database, @Nullable List<PostgreSchema> schemas, @Nullable List<PostgreTableBase> tables) {
        super(database);
        this.schemas = schemas;
        this.tables = tables;
    }

    @Nullable
    public List<PostgreSchema> getSchemas() {
        return schemas;
    }

    public void setSchemas(@Nullable List<PostgreSchema> schemas) {
        this.schemas = schemas;
    }

    @Nullable
    public List<PostgreTableBase> getTables() {
        return tables;
    }

    public void setTables(@Nullable List<PostgreTableBase> tables) {
        this.tables = tables;
    }
}
