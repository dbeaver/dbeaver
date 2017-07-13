/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;

import java.util.Collection;

/**
 * Generic struct container
 */
public interface GenericStructContainer extends DBSObjectContainer, DBSProcedureContainer
{

    @NotNull
    @Override
    GenericDataSource getDataSource();

    DBSObject getObject();

    GenericCatalog getCatalog();

    GenericSchema getSchema();

    TableCache getTableCache();

    IndexCache getIndexCache();

    PrimaryKeysCache getPrimaryKeysCache();

    ForeignKeysCache getForeignKeysCache();

    Collection<? extends GenericTable> getViews(DBRProgressMonitor monitor) throws DBException;
    Collection<? extends GenericTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericTable> getTables(DBRProgressMonitor monitor) throws DBException;

    GenericTable getTable(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<? extends GenericTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericPackage> getPackages(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException;

    GenericProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException;

    Collection<? extends GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<? extends GenericSequence> getSequences(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends GenericTrigger> getTriggers(DBRProgressMonitor monitor) throws DBException;

    Collection<? extends DBSDataType> getDataTypes(DBRProgressMonitor monitor) throws DBException;


}
