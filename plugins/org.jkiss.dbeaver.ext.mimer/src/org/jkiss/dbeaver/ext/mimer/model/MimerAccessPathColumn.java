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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

/**
 * One column of a {@link MimerAccessPath}, read from a single {@code
 * INFORMATION_SCHEMA.EXT_ACCESS_PATHS} row.
 * <p>
 * {@code columnSource} says why the column is part of this access path: {@code BASE TABLE} (an
 * ordinary declared index/key column), {@code INDEX}/{@code CONSTRAINT} (the leading column(s)
 * of a user-created index or a foreign key constraint), or {@code BASE TABLE KEY} (a primary
 * key column implicitly appended so the access path can uniquely locate a row).
 * <p>
 * {@code key} is {@code false} for a column carried along for query-covering purposes rather
 * than as part of the key itself - e.g. a trailing, collation-bearing column appended after the
 * real key columns. Read from {@code IS_KEY_COLUMN}, defaulting to {@code true} when the column
 * is absent, since pre-11.1 servers have no {@code INCLUDE} concept at all - every access-path
 * column is a key column there.
 * <p>
 * {@link #algorithm} is Mimer SQL's per-column access-path algorithm: {@code SIMPLE} for an
 * ordinary column, or a word-search/PinYin algorithm for a column created with a {@code
 * CREATE INDEX ... "col" FOR <algorithm>} clause (see {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager}/{@link MimerTableIndexColumn} for the
 * write side). This is genuinely a per-column property, not an index-wide one - a single index
 * can mix a special-algorithm column with ordinary ones (see {@link MimerAccessPath}'s own
 * Javadoc for why it isn't also exposed there).
 *
 * @author Mimer Information Technology
 */
public class MimerAccessPathColumn implements DBSObject {

    private final MimerAccessPath accessPath;
    private final String name;
    private final int ordinalPosition;
    private final String columnSource;
    private final boolean key;
    private final String algorithm;
    private final String collationSchema;
    private final String collationName;

    MimerAccessPathColumn(@NotNull MimerAccessPath accessPath, @NotNull JDBCResultSet dbResult) {
        this.accessPath = accessPath;
        this.name = JDBCUtils.safeGetString(dbResult, "COLUMN_NAME");
        this.ordinalPosition = JDBCUtils.safeGetInt(dbResult, "ORDINAL_POSITION");
        this.columnSource = JDBCUtils.safeGetStringTrimmed(dbResult, "COLUMN_SOURCE");
        // IS_KEY_COLUMN doesn't exist pre-11.1 (see MimerTable#loadAccessPaths). Only read it
        // when the query actually asked for it - otherwise we'd hit JDBCUtils' own missing-
        // column catch-and-log path for no reason.
        String keyColumn = accessPath.getDataSource().supportsIndexInclude()
            ? JDBCUtils.safeGetStringTrimmed(dbResult, "IS_KEY_COLUMN")
            : null;
        this.key = keyColumn == null || "YES".equalsIgnoreCase(keyColumn);
        this.algorithm = JDBCUtils.safeGetStringTrimmed(dbResult, "INDEX_ALGORITHM");
        this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATION_SCHEMA");
        this.collationName = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATION_NAME");
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    @Property(viewable = true, order = 3)
    public String getColumnSource() {
        return columnSource;
    }

    @Property(viewable = true, order = 4)
    public boolean isKey() {
        return key;
    }

    @Property(viewable = true, order = 5)
    public String getAlgorithm() {
        return algorithm;
    }

    @Property(viewable = true, order = 6)
    public String getCollation() {
        if (CommonUtils.isEmpty(collationName)) {
            return null;
        }
        return collationSchema + "." + collationName;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return accessPath;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return accessPath.getDataSource();
    }
}
