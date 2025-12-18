/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.starrocks.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.starrocks.StarRocksDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * StarRocks Table - represents a table within a StarRocks database.
 * Implements catalog-aware fully qualified names.
 */
public class StarRocksTable extends JDBCTable<StarRocksDataSource, StarRocksDatabase>
        implements DBPQualifiedObject {

    private static final Log log = Log.getLog(StarRocksTable.class);

    private final boolean isView;
    private String tableType;
    private String engine;
    private String comment;

    public StarRocksTable(StarRocksDatabase database, String tableName, boolean isView) {
        super(database, tableName, false);
        this.isView = isView;
    }

    @Override
    public boolean isView() {
        return isView;
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return getContainer().getDataSource();
    }

    public StarRocksCatalog getCatalog() {
        return getContainer().getCatalog();
    }

    @Property(viewable = true, order = 2)
    public String getTableType() {
        return tableType;
    }

    public void setTableType(String tableType) {
        this.tableType = tableType;
    }

    @Property(viewable = true, order = 3)
    public String getEngine() {
        return engine;
    }

    public void setEngine(String engine) {
        this.engine = engine;
    }

    @Nullable
    @Override
    @Property(viewable = true, order = 100)
    public String getDescription() {
        return comment;
    }

    public void setDescription(String comment) {
        this.comment = comment;
    }

    // ======== DBPQualifiedObject Implementation ========

    /**
     * Override to provide catalog-aware fully qualified names.
     * Format: catalog.database.table or `catalog`.`database`.`table`
     */
    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        StarRocksCatalog catalog = getCatalog();
        StarRocksDatabase database = getContainer();

        switch (context) {
            case DML:
            case DDL:
                // For SQL contexts, include catalog.database.table
                if (catalog != null) {
                    return DBUtils.getQuotedIdentifier(catalog) + "." +
                           DBUtils.getQuotedIdentifier(database) + "." +
                           DBUtils.getQuotedIdentifier(this);
                }
                // Fall through to default if no catalog
            default:
                // For UI and other contexts, use database.table
                return DBUtils.getQuotedIdentifier(database) + "." +
                       DBUtils.getQuotedIdentifier(this);
        }
    }

    // ======== Column Access ========

    @Override
    public JDBCStructCache<StarRocksDatabase, StarRocksTable, StarRocksTableColumn> getCache() {
        return getContainer().getTableCache();
    }

    @Nullable
    @Override
    public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().getTableCache().getChildren(monitor, getContainer(), this);
    }

    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return getContainer().getTableCache().getChild(monitor, getContainer(), this, attributeName);
    }

    // ======== Required Abstract Method Implementations ========

    @Nullable
    @Override
    public Collection<? extends DBSTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        // StarRocks doesn't have traditional indexes like MySQL
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        // StarRocks doesn't expose constraints the same way as MySQL
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        // StarRocks doesn't have foreign key associations
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        // StarRocks doesn't have foreign key references
        return Collections.emptyList();
    }
}
