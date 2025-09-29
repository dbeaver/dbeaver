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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;

import java.util.Collection;
import java.util.List;

/**
 * Generic struct container
 */
public interface GenericStructContainer extends DBSObjectContainer, DBSProcedureContainer {

    @NotNull
    @Override
    GenericDataSource getDataSource();

    @NotNull
    GenericStructContainer getObject();

    @Nullable
    GenericCatalog getCatalog();

    @Nullable
    GenericSchema getSchema();

    @NotNull
    TableCache getTableCache();

    @NotNull
    IndexCache getIndexCache();

    @NotNull
    ConstraintKeysCache getConstraintKeysCache();

    @NotNull
    ForeignKeysCache getForeignKeysCache();

    @NotNull
    TableTriggerCache getTableTriggerCache();

    @NotNull
    GenericObjectContainer.GenericSequenceCache getSequenceCache();

    @NotNull
    GenericObjectContainer.GenericSynonymCache getSynonymCache();

    List<? extends GenericView> getViews(@NotNull DBRProgressMonitor monitor) throws DBException;
    List<? extends GenericTable> getPhysicalTables(@NotNull DBRProgressMonitor monitor) throws DBException;

    List<? extends GenericTableBase> getTables(@NotNull DBRProgressMonitor monitor) throws DBException;

    GenericTableBase getTable(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException;

    Collection<? extends GenericTableIndex> getIndexes(@NotNull DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericPackage> getPackages(@NotNull DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericProcedure> getProcedures(@NotNull DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericProcedure> getProceduresOnly(@NotNull DBRProgressMonitor monitor) throws DBException;

    GenericProcedure getProcedure(@NotNull DBRProgressMonitor monitor, @NotNull String uniqueName) throws DBException;

    Collection<? extends GenericProcedure> getProcedures(@NotNull DBRProgressMonitor monitor, @NotNull String name) throws DBException;

    Collection<? extends GenericProcedure> getFunctionsOnly(@NotNull DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericSequence> getSequences(@NotNull DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericSynonym> getSynonyms(@NotNull DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericTrigger<?>> getTriggers(@NotNull DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericTrigger<?>> getTableTriggers(@NotNull DBRProgressMonitor monitor) throws DBException;

    Collection<? extends DBSDataType> getDataTypes(@NotNull DBRProgressMonitor monitor) throws DBException;


}
