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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@code GenericTableIndex} with correct "clustered" reporting for Mimer SQL. The driver's
 * {@code getIndexInfo()} always reports {@code tableIndexClustered} regardless of server version
 * or actual state, so {@link #resolveIndexType} corrects it eagerly in the constructor via
 * {@link MimerSchema#getIndexExtInfo}: {@link DBSIndexType#OTHER} on servers that don't support
 * clustered indexes (see {@link MimerDataSource#supportsClusteredIndexes}), the real catalog
 * value otherwise. A not-yet-persisted index is left alone - nothing to read back yet.
 * <p>
 * {@link #isClustered}/{@link #isIgnoreNulls} expose the same {@code EXT_INDEXES} data as
 * explicit properties (needed for {@code IGNORE NULLS}, which has no {@link DBSIndexType}
 * equivalent). {@link #pendingIgnoreNulls}/{@link #includedColumns} are CREATE-only fields for
 * the 11.1 {@code IGNORE NULLS}/{@code INCLUDE (col, ...)} clauses.
 *
 * @author Mimer Information Technology
 */
public class MimerTableIndex extends GenericTableIndex {

    /**
     * "Ignore Nulls" choice from the create-index dialog - only meaningful before the index is
     * persisted, since there's no catalog row yet to read it back from. See {@link #isIgnoreNulls}.
     */
    private Boolean pendingIgnoreNulls;

    /**
     * Columns for Mimer SQL 11.1's {@code CREATE INDEX ... INCLUDE (col, ...)} clause (see
     * {@link MimerDataSource#supportsIndexInclude}) - create-time-only, same reasoning as
     * {@link #pendingIgnoreNulls}; never re-hydrated on load.
     */
    private List<GenericTableColumn> includedColumns = Collections.emptyList();

    private String comment;

    /**
     * Cached wrapper for {@link #getAttributeReferences}, keyed by identity against the plain
     * {@code GenericTableIndexColumn}s {@code IndexCache} (generic, not Mimer-overridable) builds
     * for an already-persisted index. See {@link MimerTableIndexColumn}'s own "wrap" constructor
     * javadoc for why this exists.
     */
    private List<GenericTableIndexColumn> wrappedColumns;

    public MimerTableIndex(
        @NotNull GenericTableBase table,
        boolean nonUnique,
        String qualifier,
        long cardinality,
        String indexName,
        DBSIndexType indexType,
        boolean persisted
    ) {
        super(table, nonUnique, qualifier, cardinality, indexName, resolveIndexType(table, indexName, indexType, persisted), persisted);
    }

    @NotNull
    private static DBSIndexType resolveIndexType(
        @NotNull GenericTableBase table,
        String indexName,
        DBSIndexType indexType,
        boolean persisted
    ) {
        if (!(table.getDataSource() instanceof MimerDataSource ds) || !ds.supportsClusteredIndexes()) {
            return DBSIndexType.OTHER;
        }
        if (!persisted || indexName == null || !(table.getSchema() instanceof MimerSchema schema)) {
            // Not created yet - nothing to read back, keep whatever the caller (the create
            // dialog, via MimerIndexManager/MimerTable#getTableIndexTypes) already decided.
            return indexType;
        }
        try {
            return schema.getIndexExtInfo(new VoidProgressMonitor(), table.getName(), indexName).clustered()
                ? DBSIndexType.CLUSTERED : DBSIndexType.OTHER;
        } catch (DBException e) {
            // EXT_INDEXES not readable for some reason - fail open with the driver's own
            // (possibly wrong) value rather than let index construction itself fail.
            return indexType;
        }
    }

    @Property(viewable = true, order = 10)
    public boolean isClustered(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!(getDataSource() instanceof MimerDataSource ds) || !ds.supportsClusteredIndexes()) {
            return false;
        }
        return extInfo(monitor).clustered();
    }

    @Property(viewable = true, order = 11)
    public boolean isIgnoreNulls(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!isPersisted()) {
            return Boolean.TRUE.equals(pendingIgnoreNulls);
        }
        if (!(getDataSource() instanceof MimerDataSource ds) || !ds.supportsClusteredIndexes()) {
            return false;
        }
        return extInfo(monitor).ignoreNulls();
    }

    /**
     * Set from the create-index dialog only - see {@link #pendingIgnoreNulls}.
     */
    public void setIgnoreNulls(boolean ignoreNulls) {
        this.pendingIgnoreNulls = ignoreNulls;
    }

    /**
     * Set from the create-index dialog only - see {@link #includedColumns}.
     */
    public void setIncludedColumns(@NotNull List<GenericTableColumn> includedColumns) {
        this.includedColumns = includedColumns;
    }

    @NotNull
    public List<GenericTableColumn> getIncludedColumns() {
        return includedColumns;
    }

    /**
     * Wraps every column {@code IndexCache} (generic, not Mimer-overridable) built as a plain
     * {@code GenericTableIndexColumn} into a {@link MimerTableIndexColumn} instead, so the
     * properties grid shown when double-clicking an already-persisted index gets the same
     * Column/Sort Order/Algorithm treatment a not-yet-persisted one already had from the create
     * dialog - without this the grid is missing Algorithm entirely for an existing index, since
     * {@code IndexCache} has no reason to know about a Mimer-specific column subclass at all.
     * Cached per-instance ({@link #wrappedColumns}) rather than rebuilt on every call, since
     * {@link MimerTableIndexColumn}'s own read-back Algorithm lookup is itself cached per-column -
     * rebuilding the wrapper objects on every grid refresh would defeat that.
     */
    @Nullable
    @Override
    public synchronized List<GenericTableIndexColumn> getAttributeReferences(@Nullable DBRProgressMonitor monitor) {
        List<GenericTableIndexColumn> columns = super.getAttributeReferences(monitor);
        if (columns == null) {
            return null;
        }
        if (wrappedColumns == null || wrappedColumns.size() != columns.size()) {
            List<GenericTableIndexColumn> wrapped = new ArrayList<>(columns.size());
            for (GenericTableIndexColumn column : columns) {
                wrapped.add(column instanceof MimerTableIndexColumn ? column : new MimerTableIndexColumn(this, column));
            }
            wrappedColumns = wrapped;
        }
        return wrappedColumns;
    }

    @NotNull
    private MimerSchema.IndexExtInfo extInfo(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (!(getTable().getSchema() instanceof MimerSchema schema)) {
            return MimerSchema.IndexExtInfo.NONE;
        }
        return schema.getIndexExtInfo(monitor, getTable().getName(), getName());
    }

    /**
     * Hides the inherited {@code GenericTableIndex.getDescription()} - always {@code null} there,
     * but a redundant "Description" row next to the real {@link #getComment} below.
     */
    @Nullable
    @Override
    @Property(hidden = true)
    public String getDescription() {
        return super.getDescription();
    }

    /**
     * {@code COMMENT ON INDEX "schema"."name" IS '...'}. See {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager} for the write side.
     */
    @Nullable
    @Property(viewable = true, editable = true, updatable = true, length = PropertyLength.MULTILINE, order = 12)
    public String getComment(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (comment == null && isPersisted()) {
            comment = MimerUtils.readObjectComment(monitor, this, getTable().getSchema().getName(), null, getName(), "INDEX");
        }
        return comment;
    }

    public void setComment(@Nullable String comment) {
        this.comment = comment;
    }
}
