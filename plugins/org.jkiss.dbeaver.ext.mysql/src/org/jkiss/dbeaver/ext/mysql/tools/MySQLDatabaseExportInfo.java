/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ext.mysql.tools;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;

import java.util.Collection;

/**
* MySQLDatabaseExportInfo
*/
public class MySQLDatabaseExportInfo {
    @NotNull
    private MySQLCatalog database;
    @Nullable
    private Collection<MySQLTableBase> tables;

    public MySQLDatabaseExportInfo(@NotNull MySQLCatalog database, @Nullable Collection<MySQLTableBase> tables) {
        this.database = database;
        this.tables = tables;
    }

    @NotNull
    public MySQLCatalog getDatabase() {
        return database;
    }

    @Nullable
    public Collection<MySQLTableBase> getTables() {
        return tables;
    }

    @Override
    public String toString() {
        return database.getName() + " " + tables;
    }
}
