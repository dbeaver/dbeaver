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
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericTableColumn;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndex;
import org.jkiss.dbeaver.ext.generic.model.GenericTableIndexColumn;
import org.jkiss.dbeaver.ext.mimer.MimerConstants;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.Collection;

/**
 * {@link GenericTableIndexColumn} plus two CREATE-only fields: {@link #createTimeAlgorithm} for
 * Mimer SQL's per-column {@code "col" FOR <algorithm>} clause (word search / PinYin indexes),
 * and {@link #createTimeCollation} for its {@code "col" COLLATE <collation>} clause. See {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager#appendIndexColumnModifiers} for the write
 * side (uses the plain, no-monitor {@link #getAlgorithm()}/{@link #getCollation()}) and {@link
 * MimerConstants#INDEX_ALGORITHMS_FOR_CLAUSE} for the algorithm choices offered. {@code null}
 * means an ordinary column - the DDL simply omits the {@code FOR}/{@code COLLATE} clause for
 * those, since {@code SIMPLE} isn't itself a keyword {@code CREATE INDEX} accepts, only a
 * read-back value.
 * <p>
 * Two constructors, two different lifetimes. The 6-arg one is for a not-yet-persisted index
 * being built in the create dialog, where the algorithm/collation are exactly what the user
 * picked - nothing to look up. The 2-arg "wrap" one is for an already-persisted index's column,
 * used by {@link MimerTableIndex#getAttributeReferences} to upgrade the plain {@code
 * GenericTableIndexColumn}s generic's own {@code IndexCache} builds (it has no reason to know
 * about this Mimer-specific subclass) into ones that can show real column/sort-order/algorithm/
 * collation data.
 * <p>
 * That upgrade fixes display problems on an existing index's columns: the inherited "Name" and
 * "Column" properties both just showed the plain column name, confusing since an index column
 * has no name of its own distinct from the column it references (fixed by hiding {@link
 * #getName()}, keeping the real, clickable {@code getTableColumn()} as the sole "which column"
 * indicator); the boolean "Ascending" checkbox read oddly for a descending column (fixed by
 * hiding {@link #isAscending()} in favor of {@link #getSortOrder()}, a plain
 * "Ascending"/"Descending" text property); and Algorithm/Collation were missing entirely, which
 * is what the wrap constructor and {@link #getAlgorithm(DBRProgressMonitor)}/{@link
 * #getCollation(DBRProgressMonitor)} (each distinct from its own plain, no-monitor counterpart
 * the DDL builder uses) are for.
 *
 * @author Mimer Information Technology
 */
public class MimerTableIndexColumn extends GenericTableIndexColumn {

    private final String createTimeAlgorithm;
    private final String createTimeCollation;

    /**
     * Resolved lazily from the owning table's {@link MimerAccessPath}s (the same data the table's
     * own "Access Path" folder already shows) the first time {@link #getAlgorithm(DBRProgressMonitor)}
     * is called on an already-persisted column - {@link #accessPathColumnResolved} distinguishes
     * "not looked up yet" from "looked up, found nothing" so a genuinely absent access path isn't
     * re-queried on every grid refresh.
     */
    private boolean accessPathColumnResolved;
    private MimerAccessPathColumn resolvedAccessPathColumn;

    public MimerTableIndexColumn(
        @NotNull GenericTableIndex index,
        @NotNull GenericTableColumn tableColumn,
        int ordinalPosition,
        boolean ascending,
        @Nullable String algorithm,
        @Nullable String collation
    ) {
        super(index, tableColumn, ordinalPosition, ascending);
        this.createTimeAlgorithm = algorithm;
        this.createTimeCollation = collation;
    }

    /**
     * Wraps an already-persisted index's plain {@code GenericTableIndexColumn} - see the class
     * javadoc. {@code source.getTableColumn()} is never actually {@code null} in practice for a
     * real, loaded index column, despite the base class technically allowing it.
     */
    MimerTableIndexColumn(@NotNull GenericTableIndex index, @NotNull GenericTableIndexColumn source) {
        super(index, source.getTableColumn(), source.getOrdinalPosition(), source.isAscending());
        this.createTimeAlgorithm = null;
        this.createTimeCollation = null;
    }

    /**
     * The {@code FOR <algorithm>} keyword to emit for this column, or {@code null} for an
     * ordinary (simple) index column - see the class javadoc. Create-time only, deliberately not
     * a {@code @Property} - {@link #getAlgorithm(DBRProgressMonitor)} is the grid-visible one.
     */
    @Nullable
    public String getAlgorithm() {
        return createTimeAlgorithm;
    }

    /**
     * The {@code COLLATE <collation>} clause to emit for this column, or {@code null} for none -
     * create-time only, same shape as {@link #getAlgorithm()} (see {@link
     * org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager#appendIndexColumnModifiers} for the
     * write side; {@link #getCollation(DBRProgressMonitor)} is the grid-visible one for an
     * already-persisted column).
     */
    @Nullable
    public String getCollation() {
        return createTimeCollation;
    }

    /**
     * Hides the inherited "Name" property - identical to the {@code getTableColumn()} property's
     * own value (an index column has no name distinct from the column it references), just as a
     * plain, non-clickable string instead of a link to the real column. See the class javadoc.
     */
    @NotNull
    @Override
    @Property(hidden = true)
    public String getName() {
        return super.getName();
    }

    /**
     * Hides the inherited boolean "Ascending" checkbox in favor of {@link #getSortOrder()}.
     */
    @Override
    @Property(hidden = true)
    public boolean isAscending() {
        return super.isAscending();
    }

    @Property(viewable = true, order = 3)
    public String getSortOrder() {
        return isAscending() ? "Ascending" : "Descending";
    }

    /**
     * The real, per-column algorithm for an already-persisted index's column - {@code "SIMPLE"}
     * for an ordinary one (matching how {@link MimerAccessPathColumn#getAlgorithm()} itself
     * reports it), resolved from the owning table's already-loaded {@link MimerAccessPath}s (see
     * {@link MimerTable#getAccessPaths}) rather than a fresh query. For a not-yet-persisted
     * column (still in the create dialog) this just reflects {@link #getAlgorithm()} instead -
     * nothing to look up yet.
     */
    @Property(viewable = true, order = 4)
    public String getAlgorithm(@NotNull DBRProgressMonitor monitor) {
        if (!getIndex().isPersisted()) {
            return CommonUtils.isEmpty(createTimeAlgorithm) ? MimerConstants.INDEX_ALGORITHM_SIMPLE : createTimeAlgorithm;
        }
        MimerAccessPathColumn accessPathColumn = resolveAccessPathColumn(monitor);
        String algorithm = accessPathColumn == null ? null : accessPathColumn.getAlgorithm();
        return CommonUtils.isEmpty(algorithm) ? MimerConstants.INDEX_ALGORITHM_SIMPLE : algorithm;
    }

    /**
     * The real collation for an already-persisted index column, resolved the same way as {@link
     * #getAlgorithm(DBRProgressMonitor)} - from the owning table's already-loaded {@link
     * MimerAccessPath}s, via {@link MimerAccessPathColumn#getCollation()}. For a not-yet-
     * persisted column this just reflects {@link #getCollation()} instead. {@code null} for a
     * non-character column (see {@link MimerConstants#isCharacterType}) - deliberately not
     * hidden via {@code visibleIf} for that case: this is a grid-rendered property (the index's
     * Columns folder shows one row per column), and {@code visibleIf} doesn't reliably
     * re-evaluate per row there when the answer varies row to row - see {@link
     * MimerTableColumn#getCollation(DBRProgressMonitor)}'s own Javadoc for the full story. Same
     * "always show the column, let a non-eligible row's value communicate it instead" choice
     * {@link #getAlgorithm(DBRProgressMonitor)} already makes.
     */
    @Nullable
    @Property(viewable = true, order = 5)
    public String getCollation(@NotNull DBRProgressMonitor monitor) {
        if (!getIndex().isPersisted()) {
            return createTimeCollation;
        }
        MimerAccessPathColumn accessPathColumn = resolveAccessPathColumn(monitor);
        return accessPathColumn == null ? null : accessPathColumn.getCollation();
    }

    @Nullable
    private MimerAccessPathColumn resolveAccessPathColumn(@NotNull DBRProgressMonitor monitor) {
        if (accessPathColumnResolved) {
            return resolvedAccessPathColumn;
        }
        accessPathColumnResolved = true;
        if (getIndex().getTable() instanceof MimerTable mimerTable) {
            // Index names are unique, so at most one access path can match - split into two
            // single-purpose lookups (rather than one nested loop) so neither loop's body ends
            // unconditionally in a branching statement.
            MimerAccessPath path = findAccessPathByName(mimerTable.getAccessPaths(monitor), getIndex().getName());
            if (path != null) {
                resolvedAccessPathColumn = findColumnByName(path.getColumns(), getName());
            }
        }
        return resolvedAccessPathColumn;
    }

    @Nullable
    private static MimerAccessPath findAccessPathByName(@NotNull Collection<MimerAccessPath> paths, @NotNull String name) {
        for (MimerAccessPath path : paths) {
            if (path.getName().equalsIgnoreCase(name)) {
                return path;
            }
        }
        return null;
    }

    @Nullable
    private static MimerAccessPathColumn findColumnByName(@NotNull Collection<MimerAccessPathColumn> columns, @NotNull String name) {
        for (MimerAccessPathColumn column : columns) {
            if (column.getName().equalsIgnoreCase(name)) {
                return column;
            }
        }
        return null;
    }
}
