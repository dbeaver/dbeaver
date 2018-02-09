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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedure;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;

import java.util.Collection;

/**
* SQL Server schemas
*/
public class MSSQLSchema implements DBSSchema, DBPRefreshableObject, DBPSystemObject, DBSProcedureContainer {

    private final MSSQLDatabase database;
    private String schemaName;

    public MSSQLSchema(MSSQLDatabase database, String schemaName) {
        this.database = database;
        this.schemaName = schemaName;
    }

    @NotNull
    public MSSQLDatabase getDatabase() {
        return database;
    }

    @Override
    public DBSObject getParentObject() {
        return database;
    }

    @Override
    public DBPDataSource getDataSource() {
        return database.getDataSource();
    }

    @Override
    public String getName() {
        return schemaName;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public boolean isSystem() {
        return false;
    }

    @Association
    public Collection<MSSQLTable> getTables(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public Collection<? extends DBSObject> getChildren(DBRProgressMonitor monitor) throws DBException {
        return getTables(monitor);
    }

    @Override
    public DBSObject getChild(DBRProgressMonitor monitor, String childName) throws DBException {
        return null;
    }

    @Override
    public Class<? extends DBSObject> getChildType(DBRProgressMonitor monitor) throws DBException {
        return MSSQLTable.class;
    }

    @Override
    public Collection<? extends DBSProcedure> getProcedures(DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    @Override
    public DBSProcedure getProcedure(DBRProgressMonitor monitor, String uniqueName) throws DBException {
        return null;
    }

    @Override
    public void cacheStructure(DBRProgressMonitor monitor, int scope) throws DBException {
        getTables(monitor);
    }

    @Override
    public DBSObject refreshObject(DBRProgressMonitor monitor) throws DBException {
        return this;
    }

}
