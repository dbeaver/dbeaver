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
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPSystemObject;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TiberoSchema implements DBSSchema, DBSObjectContainer, DBPSystemObject, DBPRefreshableObject {

    private static final Set<String> SYSTEM_SCHEMAS = Set.of(
        "SYS",
        "SYSTEM",
        "SYSCAT",
        "SYSGIS",
        "SYSLNK",
        "SYSSSO",
        "TIBERO"
    );

    private final TiberoDataSource dataSource;
    private final String name;

    public final TiberoDataSource.TableCache tableCache = new TiberoDataSource.TableCache();
    public final TiberoDataSource.IndexCache indexCache = new TiberoDataSource.IndexCache(tableCache);
    public final TiberoDataSource.ConstraintCache constraintCache = new TiberoDataSource.ConstraintCache(tableCache);
    public final TiberoDataSource.ForeignKeyCache foreignKeyCache = new TiberoDataSource.ForeignKeyCache(tableCache);
    public final TiberoDataSource.SequenceCache sequenceCache = new TiberoDataSource.SequenceCache();
    public final TiberoDataSource.ProceduresCache proceduresCache = new TiberoDataSource.ProceduresCache();
    public final TiberoDataSource.PackageCache packageCache = new TiberoDataSource.PackageCache();
    public final TiberoDataSource.SchemaTriggerCache schemaTriggerCache = new TiberoDataSource.SchemaTriggerCache();
    public final TiberoDataSource.TableTriggerCache tableTriggerCache = new TiberoDataSource.TableTriggerCache(tableCache);

    public TiberoSchema(@NotNull TiberoDataSource dataSource, @NotNull String name) {
        this.dataSource = dataSource;
        this.name = name;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return null;
    }

    @Nullable
    @Override
    public DBSObject getParentObject() {
        return dataSource.getContainer();
    }

    @NotNull
    @Override
    public TiberoDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public boolean isSystem() {
        return SYSTEM_SCHEMAS.contains(name.toUpperCase(Locale.ENGLISH));
    }

    @NotNull
    @Association
    public Collection<TiberoTable> getTables(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<TiberoTable> tables = new ArrayList<>();
        for (TiberoTableBase obj : tableCache.getAllObjects(monitor, this)) {
            if (obj instanceof TiberoTable table) {
                tables.add(table);
            }
        }
        return tables;
    }

    @NotNull
    @Association
    public Collection<TiberoView> getViews(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<TiberoView> views = new ArrayList<>();
        for (TiberoTableBase obj : tableCache.getAllObjects(monitor, this)) {
            if (obj instanceof TiberoView view) {
                views.add(view);
            }
        }
        return views;
    }

    @Nullable
    public TiberoTable getTable(@NotNull DBRProgressMonitor monitor, @NotNull String tableName) throws DBException {
        TiberoTableBase obj = tableCache.getObject(monitor, this, tableName);
        return obj instanceof TiberoTable table ? table : null;
    }

    @Nullable
    public TiberoView getView(@NotNull DBRProgressMonitor monitor, @NotNull String viewName) throws DBException {
        TiberoTableBase obj = tableCache.getObject(monitor, this, viewName);
        return obj instanceof TiberoView view ? view : null;
    }

    @NotNull
    @Association
    public Collection<TiberoTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException {
        return indexCache.getObjects(monitor, this, null);
    }

    @NotNull
    @Association
    public Collection<TiberoSequence> getSequences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return sequenceCache.getAllObjects(monitor, this);
    }

    @NotNull
    @Association
    public Collection<TiberoPackage> getPackages(@NotNull DBRProgressMonitor monitor) throws DBException {
        return packageCache.getAllObjects(monitor, this);
    }

    @NotNull
    @Association
    public Collection<TiberoProcedure> getProcedures(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<TiberoProcedure> procs = new ArrayList<>();
        for (TiberoProcedure p : proceduresCache.getAllObjects(monitor, this)) {
            if (p.getProcedureType() == org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType.PROCEDURE) {
                procs.add(p);
            }
        }
        return procs;
    }

    @NotNull
    @Association
    public Collection<TiberoProcedure> getFunctions(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<TiberoProcedure> funcs = new ArrayList<>();
        for (TiberoProcedure p : proceduresCache.getAllObjects(monitor, this)) {
            if (p.getProcedureType() == org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType.FUNCTION) {
                funcs.add(p);
            }
        }
        return funcs;
    }

    @NotNull
    @Association
    public Collection<TiberoSchemaTrigger> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return schemaTriggerCache.getAllObjects(monitor, this);
    }

    @NotNull
    @Association
    public Collection<TiberoTableTrigger> getTableTriggers(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tableTriggerCache.getObjects(monitor, this, null);
    }

    @Nullable
    @Override
    public Collection<? extends DBSObject> getChildren(@NotNull DBRProgressMonitor monitor) throws DBException {
        return tableCache.getAllObjects(monitor, this);
    }

    @Nullable
    @Override
    public DBSObject getChild(@NotNull DBRProgressMonitor monitor, @NotNull String childName) throws DBException {
        return tableCache.getObject(monitor, this, childName);
    }

    @NotNull
    @Override
    public Class<? extends DBSObject> getPrimaryChildType(@Nullable DBRProgressMonitor monitor) {
        return TiberoTable.class;
    }

    @Override
    public void cacheStructure(@NotNull DBRProgressMonitor monitor, int scope) throws DBException {
        tableCache.getAllObjects(monitor, this);
        if ((scope & STRUCT_ATTRIBUTES) != 0) {
            tableCache.loadChildren(monitor, this, null);
        }
        if ((scope & STRUCT_ASSOCIATIONS) != 0) {
            indexCache.getObjects(monitor, this, null);
            constraintCache.getObjects(monitor, this, null);
            foreignKeyCache.getObjects(monitor, this, null);
            packageCache.getAllObjects(monitor, this);
            schemaTriggerCache.getAllObjects(monitor, this);
            tableTriggerCache.getObjects(monitor, this, null);
        }
    }

    @Override
    public synchronized DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        tableCache.clearCache();
        indexCache.clearCache();
        constraintCache.clearCache();
        foreignKeyCache.clearCache();
        sequenceCache.clearCache();
        proceduresCache.clearCache();
        packageCache.clearCache();
        schemaTriggerCache.clearCache();
        tableTriggerCache.clearCache();
        return this;
    }
}
