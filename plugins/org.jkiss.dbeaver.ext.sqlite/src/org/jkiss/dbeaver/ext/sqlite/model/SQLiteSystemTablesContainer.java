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
package org.jkiss.dbeaver.ext.sqlite.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractAttribute;
import org.jkiss.dbeaver.model.impl.struct.AbstractTable;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Static scope of well-known SQLite system tables exposed to the SQL semantic analyzer
 * via {@link org.jkiss.dbeaver.model.struct.DBSVisibilityScopeProvider}.
 *
 * <p>The semantic analyzer first looks up references against the regular datasource
 * containers (real, currently-loaded tables). If a name does not resolve there, the
 * connection context falls through to the public scopes provided by the datasource —
 * this container — which lets queries against SQLite system tables resolve cleanly
 * even when the table does not currently exist in the database. Notable cases:
 * {@code dbstat} is a virtual table available only when SQLite is built with
 * {@code SQLITE_ENABLE_DBSTAT_VTAB}; {@code sqlite_stat1..4} only materialize after
 * an {@code ANALYZE}; {@code sqlite_sequence} only appears once a table with an
 * {@code AUTOINCREMENT} column is created.
 *
 * <p>Per the design discussion on the prior attempt (issue #35430, reverted in
 * commit 15ad9c3), no metadata queries are issued — columns are declared statically
 * from the SQLite documentation.
 */
public final class SQLiteSystemTablesContainer implements DBSObjectContainer {

    private static final String TYPE_TEXT = "TEXT";
    private static final String TYPE_INTEGER = "INTEGER";
    private static final String TYPE_BLOB = "BLOB";

    // sqlite_master / sqlite_schema and the temp variants share the same schema.
    private static final List<String[]> COLS_MASTER = List.of(
        col("type", TYPE_TEXT),
        col("name", TYPE_TEXT),
        col("tbl_name", TYPE_TEXT),
        col("rootpage", TYPE_INTEGER),
        col("sql", TYPE_TEXT)
    );

    // sqlite_stat3 and sqlite_stat4 share the same schema.
    private static final List<String[]> COLS_STAT34 = List.of(
        col("tbl", TYPE_TEXT),
        col("idx", TYPE_TEXT),
        col("neq", TYPE_TEXT),
        col("nlt", TYPE_TEXT),
        col("ndlt", TYPE_TEXT),
        col("sample", TYPE_BLOB)
    );

    private final SQLiteDataSource dataSource;
    private final Map<String, SQLiteSystemTable> tablesByLowerName;

    public SQLiteSystemTablesContainer(@NotNull SQLiteDataSource dataSource) {
        this.dataSource = dataSource;
        Map<String, SQLiteSystemTable> tables = new LinkedHashMap<>();
        addTable(tables, "sqlite_master", COLS_MASTER);
        addTable(tables, "sqlite_schema", COLS_MASTER);
        addTable(tables, "sqlite_temp_master", COLS_MASTER);
        addTable(tables, "sqlite_temp_schema", COLS_MASTER);
        addTable(tables, "sqlite_sequence", List.of(
            col("name", TYPE_TEXT),
            col("seq", TYPE_INTEGER)
        ));
        addTable(tables, "dbstat", List.of(
            col("name", TYPE_TEXT),
            col("path", TYPE_TEXT),
            col("pageno", TYPE_INTEGER),
            col("pagetype", TYPE_TEXT),
            col("ncell", TYPE_INTEGER),
            col("payload", TYPE_INTEGER),
            col("unused", TYPE_INTEGER),
            col("mx_payload", TYPE_INTEGER),
            col("pgoffset", TYPE_INTEGER),
            col("pgsize", TYPE_INTEGER)
        ));
        addTable(tables, "sqlite_stat1", List.of(
            col("tbl", TYPE_TEXT),
            col("idx", TYPE_TEXT),
            col("stat", TYPE_TEXT)
        ));
        addTable(tables, "sqlite_stat2", List.of(
            col("tbl", TYPE_TEXT),
            col("idx", TYPE_TEXT),
            col("sampleno", TYPE_INTEGER),
            col("sample", TYPE_TEXT)
        ));
        addTable(tables, "sqlite_stat3", COLS_STAT34);
        addTable(tables, "sqlite_stat4", COLS_STAT34);
        this.tablesByLowerName = Collections.unmodifiableMap(tables);
    }

    private static String[] col(@NotNull String name, @NotNull String type) {
        return new String[]{name, type};
    }

    private void addTable(@NotNull Map<String, SQLiteSystemTable> tables, @NotNull String name, @NotNull List<String[]> columns) {
        tables.put(name, new SQLiteSystemTable(this, name, columns));
    }

    @NotNull
    @Override
    public String getName() {
        return dataSource.getName();
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
        return dataSource;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @NotNull
    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) {
        return tablesByLowerName.values();
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) {
        return tablesByLowerName.get(childName.toLowerCase(Locale.ENGLISH));
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return SQLiteSystemTable.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) {
        // Static, in-memory: nothing to cache.
    }

    /**
     * Statically-declared SQLite system table. Issues no metadata queries.
     */
    public static final class SQLiteSystemTable extends AbstractTable<SQLiteDataSource, SQLiteSystemTablesContainer> {
        private final List<SQLiteSystemTableColumn> attributes;
        private final Map<String, SQLiteSystemTableColumn> attributesByLowerName;

        SQLiteSystemTable(
            @NotNull SQLiteSystemTablesContainer container,
            @NotNull String tableName,
            @NotNull List<String[]> columnSpecs
        ) {
            super(container, tableName);
            List<SQLiteSystemTableColumn> cols = new ArrayList<>(columnSpecs.size());
            Map<String, SQLiteSystemTableColumn> byLowerName = new LinkedHashMap<>();
            int ordinal = 0;
            for (String[] spec : columnSpecs) {
                SQLiteSystemTableColumn column = new SQLiteSystemTableColumn(this, spec[0], spec[1], ordinal++);
                cols.add(column);
                byLowerName.put(spec[0].toLowerCase(Locale.ENGLISH), column);
            }
            this.attributes = Collections.unmodifiableList(cols);
            this.attributesByLowerName = Collections.unmodifiableMap(byLowerName);
        }

        @Override
        public boolean isView() {
            return false;
        }

        @Nullable
        @Override
        public String getDescription() {
            return null;
        }

        @Nullable
        @Override
        public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) {
            return attributes;
        }

        @Nullable
        @Override
        public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) {
            return attributesByLowerName.get(attributeName.toLowerCase(Locale.ENGLISH));
        }

        @Nullable
        @Override
        public Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) {
            return null;
        }

        @Nullable
        @Override
        public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) {
            return null;
        }

        @Nullable
        @Override
        public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) {
            return null;
        }

        @Override
        public Collection<? extends DBSTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public String getFullyQualifiedName(DBPEvaluationContext context) {
            return DBUtils.getQuotedIdentifier(getDataSource(), getName());
        }
    }

    /**
     * Statically-declared column of a SQLite system table.
     */
    public static final class SQLiteSystemTableColumn extends AbstractAttribute implements DBSTableColumn {
        private final SQLiteSystemTable parentTable;

        SQLiteSystemTableColumn(
            @NotNull SQLiteSystemTable parent,
            @NotNull String name,
            @NotNull String typeName,
            int ordinalPosition
        ) {
            super(name, typeName, sqlTypeFor(typeName), ordinalPosition, 0L, null, null, false, false);
            this.parentTable = parent;
        }

        @NotNull
        @Override
        public DBSEntity getParentObject() {
            return parentTable;
        }

        @NotNull
        @Override
        public DBPDataSource getDataSource() {
            return parentTable.getDataSource();
        }

        @Nullable
        @Override
        public String getDefaultValue() {
            return null;
        }

        private static int sqlTypeFor(@NotNull String typeName) {
            return switch (typeName) {
                case TYPE_INTEGER -> Types.BIGINT;
                case TYPE_BLOB -> Types.BLOB;
                default -> Types.VARCHAR;
            };
        }
    }
}
