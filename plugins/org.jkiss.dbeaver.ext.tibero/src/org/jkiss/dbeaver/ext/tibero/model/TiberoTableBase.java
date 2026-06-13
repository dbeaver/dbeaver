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
package org.jkiss.dbeaver.ext.tibero.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructCache;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTrigger;

import java.util.Collection;
import java.util.List;

public abstract class TiberoTableBase extends JDBCTable<TiberoDataSource, TiberoSchema> {

    private final String tableType;
    private final String description;

    protected TiberoTableBase(
        @NotNull TiberoSchema schema,
        @NotNull String name,
        @NotNull String tableType,
        @Nullable String description
    ) {
        super(schema, name, true);
        this.tableType = tableType;
        this.description = description;
    }

    @NotNull
    public String getTableType() {
        return tableType;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public JDBCStructCache<TiberoSchema, TiberoTableBase, TiberoTableColumn> getCache() {
        return getContainer().tableCache;
    }

    @Nullable
    @Override
    @Association
    public List<TiberoTableColumn> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().tableCache.getChildren(monitor, getContainer(), this);
    }

    @Nullable
    @Override
    public TiberoTableColumn getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) throws DBException {
        return getContainer().tableCache.getChild(monitor, getContainer(), this, attributeName);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(@NotNull DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(), getContainer(), this);
    }

    @Nullable
    @Override
    public Collection<? extends DBSTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSTableConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Nullable
    @Override
    @Association
    public List<? extends DBSTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getContainer().tableTriggerCache.getObjects(monitor, getContainer(), this);
    }

    @Nullable
    protected DBSObject refreshTableObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (this instanceof TiberoTable table) {
            getContainer().indexCache.clearObjectCache(table);
            getContainer().constraintCache.clearObjectCache(table);
            getContainer().foreignKeyCache.clearObjectCache(table);
        }
        getContainer().tableTriggerCache.clearObjectCache(this);
        getContainer().tableCache.removeObject(this, true);
        return getContainer().tableCache.getObject(monitor, getContainer(), getName());
    }
}
