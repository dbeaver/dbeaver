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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSystemInfoObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;

public class PostgreSetting implements DBSObject, DBPSystemInfoObject {
    private final PostgreDataSource database;
    private final String name;
    private final String value;
    private final String unit;
    private final String description;

    protected PostgreSetting(PostgreDataSource database, ResultSet dbResult) {
        this.database = database;
        this.name = JDBCUtils.safeGetString(dbResult, "name");
        this.value = JDBCUtils.safeGetString(dbResult, "setting");
        if (database.isServerVersionAtLeast(8, 2)) {
            this.unit = JDBCUtils.safeGetString(dbResult, "unit");
        } else {
            this.unit = null;
        }
        this.description = JDBCUtils.safeGetString(dbResult, "short_desc");
    }

    @NotNull
    @Property(viewable = true, order = 1)
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Property(viewable = true, order = 2)
    public String getValue() {
        return value;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public String getUnit() {
        return unit;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return database.getDataSource();
    }
}
