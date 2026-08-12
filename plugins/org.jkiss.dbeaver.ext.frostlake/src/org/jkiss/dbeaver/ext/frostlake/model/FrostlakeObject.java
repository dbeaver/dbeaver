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
package org.jkiss.dbeaver.ext.frostlake.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One row of a Frostlake {@code SHOW} listing, presented as a navigator object.
 *
 * <p>This single class serves every kind in {@link FrostlakeObjectKind} — stages, pipes, streams,
 * tasks and the rest — because their listings agree on the columns that identify an object
 * ({@code name}, {@code created_on}, {@code comment}, {@code schema_name}) and differ only in what
 * they add. The named four become the object's identity; everything else is kept as-is and shown in
 * the Properties tab, so a TASK with 27 columns and a FILE FORMAT with 9 both arrive complete without
 * either needing a class.
 *
 * <p>Read-only by design: Frostlake's DDL is Snowflake's, and the SQL editor is a better place to
 * write it than a generated form would be.
 */
public class FrostlakeObject implements DBSObject, DBPNamedObject2, DBPSaveableObject {

    /** The schema for a schema-scoped kind, or the data source for an account-level one. */
    private final DBSObject parent;
    private final DBPDataSource dataSource;
    private final FrostlakeObjectKind kind;
    private String name;
    private String description;
    private String createdOn;
    /** Every remaining column of the SHOW row, in the order the engine returned them. */
    private final Map<String, Object> attributes = new LinkedHashMap<>();
    private boolean persisted;

    public FrostlakeObject(@NotNull DBSObject parent,
                           @NotNull DBPDataSource dataSource,
                           @NotNull FrostlakeObjectKind kind,
                           @NotNull ResultSet dbResult) throws SQLException {
        this.parent = parent;
        this.dataSource = dataSource;
        this.kind = kind;
        this.name = CommonUtils.notEmpty(JDBCUtils.safeGetString(dbResult, "name"));
        this.description = JDBCUtils.safeGetString(dbResult, "comment");
        this.createdOn = JDBCUtils.safeGetString(dbResult, "created_on");
        this.persisted = true;
        readRemainingColumns(dbResult);
    }

    /**
     * Keep whatever the listing carried beyond the identifying four. Done positionally off the result
     * metadata rather than from a per-kind column list, which is what lets a new kind cost nothing.
     */
    private void readRemainingColumns(@NotNull ResultSet dbResult) throws SQLException {
        final ResultSetMetaData meta = dbResult.getMetaData();
        for (int i = 1; i <= meta.getColumnCount(); i++) {
            final String column = meta.getColumnName(i);
            if ("name".equalsIgnoreCase(column)
                || "comment".equalsIgnoreCase(column)
                || "created_on".equalsIgnoreCase(column)
                || "schema_name".equalsIgnoreCase(column)) {
                continue;
            }
            attributes.put(column, dbResult.getObject(i));
        }
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Override
    public void setName(@NotNull String name) {
        this.name = name;
    }

    @NotNull
    public FrostlakeObjectKind getKind() {
        return kind;
    }

    @Property(viewable = true, order = 2)
    public String getCreatedOn() {
        return createdOn;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /** The rest of the SHOW row — surfaced so the Properties tab is populated for every kind. */
    @NotNull
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return parent;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isPersisted() {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted) {
        this.persisted = persisted;
    }
}
