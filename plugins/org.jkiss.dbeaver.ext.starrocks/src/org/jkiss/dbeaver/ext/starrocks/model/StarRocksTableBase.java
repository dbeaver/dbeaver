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
import org.jkiss.dbeaver.ext.starrocks.StarRocksDataSource;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
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
 * StarRocks Table/View base class - abstract base for both tables and views.
 * Implements catalog-aware fully qualified names.
 */
public abstract class StarRocksTableBase extends JDBCTable<StarRocksDataSource, StarRocksDatabase>
        implements DBPQualifiedObject {

    public StarRocksTableBase(StarRocksDatabase database, String tableName, boolean persisted) {
        super(database, tableName, persisted);
    }

    @NotNull
    @Override
    public StarRocksDataSource getDataSource() {
        return getContainer().getDataSource();
    }

    public StarRocksCatalog getCatalog() {
        return getContainer().getCatalog();
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        StarRocksCatalog catalog = getCatalog();
        StarRocksDatabase database = getContainer();

        switch (context) {
            case DML:
            case DDL:
                if (catalog != null) {
                    return DBUtils.getQuotedIdentifier(catalog) + "." +
                           DBUtils.getQuotedIdentifier(database) + "." +
                           DBUtils.getQuotedIdentifier(this);
                }
            default:
                return DBUtils.getQuotedIdentifier(database) + "." +
                       DBUtils.getQuotedIdentifier(this);
        }
    }

    @Override
    public JDBCStructCache<StarRocksDatabase, StarRocksTableBase, StarRocksTableColumn> getCache() {
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

    @Nullable
    @Override
    public Collection<? extends DBSTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return Collections.emptyList();
    }
}
