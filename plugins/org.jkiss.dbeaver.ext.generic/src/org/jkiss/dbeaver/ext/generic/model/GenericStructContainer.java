/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
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

    Collection<GenericTable> getViews(DBRProgressMonitor monitor) throws DBException;
    Collection<GenericTable> getPhysicalTables(DBRProgressMonitor monitor) throws DBException;

    Collection<GenericTable> getTables(DBRProgressMonitor monitor) throws DBException;

    GenericTable getTable(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<GenericTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException;

    Collection<GenericPackage> getPackages(DBRProgressMonitor monitor) throws DBException;

    Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException;

    GenericProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException;

    Collection<GenericProcedure> getProcedures(DBRProgressMonitor monitor, String name) throws DBException;

    Collection<GenericSequence> getSequences(DBRProgressMonitor monitor) throws DBException;

}
