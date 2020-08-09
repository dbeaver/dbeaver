/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
public interface GenericStructContainer extends DBSObjectContainer, DBSProcedureContainer
{

    @NotNull
    @Override
    GenericDataSource getDataSource();

    GenericStructContainer getObject();

    GenericCatalog getCatalog();

    GenericSchema getSchema();

    TableCache getTableCache();

    IndexCache getIndexCache();

    ConstraintKeysCache getConstraintKeysCache();

    ForeignKeysCache getForeignKeysCache();

    Collection<? extends GenericTableBase> getViews(DBRProgressMonitor monitor) throws DBException;
    List<? extends GenericTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException;

    List<? extends GenericTableBase> getTables(DBRProgressMonitor monitor) throws DBException;

    GenericTableBase getTable(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<? extends GenericTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericPackage> getPackages(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericProcedure> getProceduresOnly(DBRProgressMonitor monitor) throws DBException;

    GenericProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException;

    Collection<? extends GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<? extends GenericProcedure> getFunctionsOnly(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericSequence> getSequences(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericSynonym> getSynonyms(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericTrigger> getTableTriggers(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException;


}
