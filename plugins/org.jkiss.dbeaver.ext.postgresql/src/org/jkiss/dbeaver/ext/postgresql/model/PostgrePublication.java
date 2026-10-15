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
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PostgrePublication extends PostgreReplicationObject {
    public static final String PROP_ID_CREATION_TABLES = "creationTables";

    private boolean allTables;
    private boolean publishInsert = true;
    private boolean publishUpdate = true;
    private boolean publishDelete = true;
    private boolean publishTruncate = true;
    private boolean publishViaPartitionRoot;
    private List<PostgreTable> tables;

    public PostgrePublication(@NotNull PostgreDatabase database, @NotNull String name) {
        super(database, name);
        tables = new ArrayList<>();
    }

    public PostgrePublication(@NotNull PostgreDatabase database, @NotNull ResultSet result) {
        super(database, result, "pubname", "pubowner");
        allTables = JDBCUtils.safeGetBoolean(result, "puballtables");
        publishInsert = JDBCUtils.safeGetBoolean(result, "pubinsert");
        publishUpdate = JDBCUtils.safeGetBoolean(result, "pubupdate");
        publishDelete = JDBCUtils.safeGetBoolean(result, "pubdelete");
        publishTruncate = JDBCUtils.safeGetBoolean(result, "pubtruncate");
        publishViaPartitionRoot = JDBCUtils.safeGetBoolean(result, "pubviaroot");
    }

    @Property(viewable = true, editable = true, order = 4)
    public boolean isAllTables() {
        return allTables;
    }

    public void setAllTables(boolean allTables) {
        this.allTables = allTables;
    }

    @Property(viewable = true, editable = true, order = 5)
    public boolean isPublishInsert() {
        return publishInsert;
    }

    public void setPublishInsert(boolean publishInsert) {
        this.publishInsert = publishInsert;
    }

    @Property(viewable = true, editable = true, order = 6)
    public boolean isPublishUpdate() {
        return publishUpdate;
    }

    public void setPublishUpdate(boolean publishUpdate) {
        this.publishUpdate = publishUpdate;
    }

    @Property(viewable = true, editable = true, order = 7)
    public boolean isPublishDelete() {
        return publishDelete;
    }

    public void setPublishDelete(boolean publishDelete) {
        this.publishDelete = publishDelete;
    }

    @Property(viewable = true, editable = true, order = 8)
    public boolean isPublishTruncate() {
        return publishTruncate;
    }

    public void setPublishTruncate(boolean publishTruncate) {
        this.publishTruncate = publishTruncate;
    }

    @Property(viewable = true, editable = true, order = 9)
    public boolean isPublishViaPartitionRoot() {
        return publishViaPartitionRoot;
    }

    public void setPublishViaPartitionRoot(boolean publishViaPartitionRoot) {
        this.publishViaPartitionRoot = publishViaPartitionRoot;
    }

    @NotNull
    @Association
    public List<PostgreTable> getTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (tables == null) {
            List<PostgreTable> loaded = new ArrayList<>();
            try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read publication tables")) {
                try (JDBCPreparedStatement statement = session.prepareStatement(
                    "SELECT c.oid, c.relnamespace FROM pg_catalog.pg_publication_tables pt " +
                    "JOIN pg_catalog.pg_namespace n ON n.nspname=pt.schemaname " +
                    "JOIN pg_catalog.pg_class c ON c.relnamespace=n.oid AND c.relname=pt.tablename " +
                    "WHERE pt.pubname=? ORDER BY pt.schemaname, pt.tablename"
                )) {
                    statement.setString(1, getName());
                    try (JDBCResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            PostgreTableBase table = getDatabase().findTable(monitor, result.getLong("relnamespace"), result.getLong("oid"));
                            if (table instanceof PostgreTable publicationTable) {
                                loaded.add(publicationTable);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBDatabaseException("Error reading publication tables", e, getDataSource());
            }
            tables = loaded;
        }
        return tables;
    }

    public void setTables(@NotNull List<PostgreTable> tables) {
        this.tables = new ArrayList<>(tables);
    }

    @NotNull
    @Property(editable = true, hidden = true)
    public List<PostgreTable> getCreationTables() {
        return tables == null ? List.of() : List.copyOf(tables);
    }

    public void setCreationTables(@NotNull List<PostgreTable> tables) {
        setTables(tables);
    }

    @NotNull
    public static List<PostgreTable> mergeTableSelection(
        @NotNull List<PostgreTable> currentTables, @NotNull PostgreTable table, boolean checked
    ) {
        // Change only the toggled table: the viewer may hide other checked tables behind its search filter.
        LinkedHashSet<PostgreTable> selected = new LinkedHashSet<>(currentTables);
        if (checked) {
            selected.add(table);
        } else {
            selected.remove(table);
        }
        return List.copyOf(selected);
    }

    @NotNull
    @Override
    public String getObjectDefinitionText(@NotNull DBRProgressMonitor monitor, @NotNull Map<String, Object> options) throws DBException {
        if (getName().isBlank()) {
            throw new DBException("Publication name cannot be empty");
        }
        if (!isPersisted() && allTables && !getTables(monitor).isEmpty()) {
            throw new DBException("FOR ALL TABLES cannot be combined with a table list");
        }
        StringBuilder ddl = new StringBuilder("CREATE PUBLICATION ").append(DBUtils.getQuotedIdentifier(this));
        if (allTables) {
            ddl.append(" FOR ALL TABLES");
        } else {
            // A newly saved object does not have its catalog OID until metadata is refreshed.
            boolean readCatalog = isPersisted() && getObjectId() != 0;
            String membership = readCatalog ? readMembership(monitor) : getTables(monitor).stream()
                .map(table -> table.getFullyQualifiedName(DBPEvaluationContext.DDL))
                .collect(Collectors.joining(", "));
            if (!membership.isEmpty()) {
                ddl.append(readCatalog ? " FOR " : " FOR TABLE ").append(membership);
            }
        }
        List<String> operations = new ArrayList<>();
        if (publishInsert) {
            operations.add("insert");
        }
        if (publishUpdate) {
            operations.add("update");
        }
        if (publishDelete) {
            operations.add("delete");
        }
        if (publishTruncate) {
            operations.add("truncate");
        }
        ddl.append("\nWITH (publish = ").append(SQLUtils.quoteString(this, String.join(", ", operations)))
            .append(", publish_via_partition_root = ").append(publishViaPartitionRoot).append(");");
        return ddl.toString();
    }

    @NotNull
    private String readMembership(@NotNull DBRProgressMonitor monitor) throws DBException {
        boolean supportsFilters = getDataSource().isServerVersionAtLeast(15, 0);
        // Use explicit membership, not pg_publication_tables: that view expands partitions and schema publications.
        // ONLY prevents replay from adding inheritance descendants absent from the explicit catalog membership.
        String sql = "SELECT " + (supportsFilters ? "'TABLE ONLY ' || " : "'ONLY ' || ") + "quote_ident(n.nspname) || '.' || quote_ident(c.relname)" +
            (supportsFilters ?
                " || CASE WHEN pr.prattrs IS NULL THEN '' ELSE ' (' || " +
                    "(SELECT string_agg(quote_ident(a.attname), ', ' ORDER BY u.ord) " +
                    "FROM unnest(pr.prattrs) WITH ORDINALITY u(attnum, ord) " +
                    "JOIN pg_catalog.pg_attribute a ON a.attrelid=pr.prrelid AND a.attnum=u.attnum) || ')' END" +
                    " || CASE WHEN pr.prqual IS NULL THEN '' ELSE ' WHERE (' || pg_get_expr(pr.prqual, pr.prrelid) || ')' END" : "") +
            " AS definition FROM pg_catalog.pg_publication_rel pr " +
            "JOIN pg_catalog.pg_class c ON c.oid=pr.prrelid " +
            "JOIN pg_catalog.pg_namespace n ON n.oid=c.relnamespace WHERE pr.prpubid=?" +
            (supportsFilters ? " UNION ALL SELECT 'TABLES IN SCHEMA ' || quote_ident(n.nspname) " +
                "FROM pg_catalog.pg_publication_namespace pn " +
                "JOIN pg_catalog.pg_namespace n ON n.oid=pn.pnnspid WHERE pn.pnpubid=?" : "") +
            " ORDER BY 1";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read publication definition")) {
            try (JDBCPreparedStatement statement = session.prepareStatement(sql)) {
                statement.setLong(1, getObjectId());
                if (supportsFilters) {
                    statement.setLong(2, getObjectId());
                }
                List<String> definitions = new ArrayList<>();
                try (JDBCResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        definitions.add(result.getString(1));
                    }
                }
                String membership = String.join(", ", definitions);
                return !supportsFilters && !membership.isEmpty() ? "TABLE " + membership : membership;
            }
        } catch (SQLException e) {
            throw new DBDatabaseException("Error reading publication definition", e, getDataSource());
        }
    }

    @Nullable
    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getDatabase().getPublicationCache().refreshObject(monitor, getDatabase(), this);
    }

    @NotNull
    public List<PostgreTable> getAvailableTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<PostgreTable> available = new ArrayList<>();
        for (PostgreSchema schema : getDatabase().getSchemas(monitor)) {
            if (!schema.isSystem()) {
                for (PostgreTable table : schema.getTables(monitor)) {
                    if (table.getPersistence() == PostgreTablePersistence.PERMANENT) {
                        available.add(table);
                    }
                }
            }
        }
        return available;
    }
}
