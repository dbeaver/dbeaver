/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;

public abstract class PostgreReplicationObject implements PostgreObject, PostgreScriptObject,
    DBPNamedObject2, DBPSaveableObject, DBPRefreshableObject {

    private final PostgreDatabase database;
    private String name;
    private long objectId;
    private long ownerId;
    private String description;
    private boolean persisted;

    protected PostgreReplicationObject(@NotNull PostgreDatabase database, @NotNull String name) {
        this.database = database;
        this.name = name;
    }

    protected PostgreReplicationObject(
        @NotNull PostgreDatabase database, @NotNull ResultSet result, @NotNull String nameColumn, @NotNull String ownerColumn
    ) {
        this(database, JDBCUtils.safeGetString(result, nameColumn));
        objectId = JDBCUtils.safeGetLong(result, "oid");
        ownerId = JDBCUtils.safeGetLong(result, ownerColumn);
        description = JDBCUtils.safeGetString(result, "description");
        persisted = true;
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @Override
    @Property(viewable = true, order = 2)
    public long getObjectId() {
        return objectId;
    }

    @Nullable
    @Property(viewable = true, order = 3)
    public PostgreRole getOwner(@NotNull DBRProgressMonitor monitor) throws DBException {
        return ownerId == 0 ? null : database.getRoleById(monitor, ownerId);
    }

    @Nullable
    @Override
    @Property(order = 100)
    public String getDescription() {
        return description;
    }

    @NotNull
    @Override
    public PostgreDatabase getDatabase() {
        return database;
    }

    @NotNull
    @Override
    public PostgreDatabase getParentObject() {
        return database;
    }

    @NotNull
    @Override
    public PostgreDataSource getDataSource() {
        return database.getDataSource();
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }

    @Override
    public void setObjectDefinitionText(@NotNull String sourceText) {
        // Replication DDL is generated from metadata; the source viewer is read-only.
    }
}
