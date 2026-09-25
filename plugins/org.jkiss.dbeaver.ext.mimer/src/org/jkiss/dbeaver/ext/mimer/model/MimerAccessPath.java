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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.util.ArrayList;
import java.util.List;

/**
 * One access path Mimer SQL's optimizer can use against a table, read from {@code
 * INFORMATION_SCHEMA.EXT_ACCESS_PATHS} - Mimer SQL's unified view of real indexes, the table's
 * primary key, and (when its column differs from the primary key's) a foreign key. Unlike the
 * generic "Indexes" folder, this is the accurate source: Mimer SQL does not treat a primary key as
 * an index, and the driver's raw {@code getIndexInfo()} also reports internal PK/FK structures
 * that don't belong there (see {@link MimerTable#isStandaloneIndex}). Rows are one per column,
 * grouped client-side by {@code INDEX_NAME} (see {@link MimerTable#loadAccessPaths}).
 * <p>
 * {@code IS_CLUSTERED} is physical row-storage clustering, not the same thing as the
 * user-facing {@code CREATE CLUSTERED INDEX} syntax (see {@link MimerTableIndex}). It shows
 * which access path currently owns the clustering - a bare primary key, until a real
 * {@code CREATE CLUSTERED INDEX} takes over.
 * <p>
 * The column only exists on {@code EXT_ACCESS_PATHS} from Mimer SQL 11.1 on.
 * {@link MimerTable#loadAccessPaths} already leaves it out of the query
 * pre-11.1, and this constructor checks the same version guard before reading it, defaulting
 * to {@code false} rather than reading a column that isn't there.
 * <p>
 * {@code INDEX_ALGORITHM} (word search / PinYin, see {@link
 * org.jkiss.dbeaver.ext.mimer.edit.MimerIndexManager}) is a per-COLUMN property in Mimer SQL, not
 * an index-wide one - exposed on {@link MimerAccessPathColumn}, not here, since a single index can
 * mix a special-algorithm column with ordinary ones (e.g. the user's own {@code "id" ASC, c1 DESC,
 * "c2" FOR WORD_SEARCH DESC} example) and an index-level summary would only ever reflect whichever
 * column happened to load first.
 *
 * @author Mimer Information Technology
 */
public class MimerAccessPath implements DBSObject {

    private final GenericTableBase table;
    private final String name;
    private final String type;
    private final boolean clustered;
    private final List<MimerAccessPathColumn> columns = new ArrayList<>();

    MimerAccessPath(@NotNull GenericTableBase table, @NotNull JDBCResultSet dbResult) {
        this.table = table;
        this.name = JDBCUtils.safeGetString(dbResult, "INDEX_NAME");
        this.type = JDBCUtils.safeGetStringTrimmed(dbResult, "INDEX_TYPE");
        // IS_CLUSTERED isn't in the result set at all pre-11.1 (see MimerTable#loadAccessPaths) -
        // only attempt the read when the query actually asked for it, rather than let the
        // missing-column exception fall through JDBCUtils' own catch-and-log path.
        MimerDataSource ds = table.getDataSource() instanceof MimerDataSource mds ? mds : null;
        boolean clusteredColumnPresent = ds == null || ds.supportsClusteredIndexes();
        this.clustered = clusteredColumnPresent
            && "YES".equalsIgnoreCase(JDBCUtils.safeGetStringTrimmed(dbResult, "IS_CLUSTERED"));
    }

    void addColumn(@NotNull JDBCResultSet dbResult) {
        columns.add(new MimerAccessPathColumn(this, dbResult));
    }

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 2)
    public String getType() {
        return type;
    }

    @Property(viewable = true, order = 3)
    public boolean isClustered() {
        return clustered;
    }

    /**
     * Whether the driver's own {@code getIndexInfo()} entry sharing this name should still show
     * up in the plain "Indexes" folder - only real, user-created indexes should (see {@link
     * MimerTable#isStandaloneIndex}); a primary key's or foreign key's own access path is shown
     * here instead, not duplicated there.
     */
    boolean isStandaloneIndex() {
        return "INDEX".equalsIgnoreCase(type) || "UNIQUE INDEX".equalsIgnoreCase(type);
    }

    @Association
    public List<MimerAccessPathColumn> getColumns() {
        return columns;
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
        return table;
    }

    @NotNull
    @Override
    public MimerDataSource getDataSource() {
        return (MimerDataSource) table.getDataSource();
    }

    @NotNull
    public GenericTableBase getTable() {
        return table;
    }
}
