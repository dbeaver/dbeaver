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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

/**
 * Object type
 */
public enum SQLServerObjectType implements DBSObjectType {

	AF ("AF", null, "Aggregate function (CLR)"),
	C ("C", SQLServerTableCheckConstraint.class, "CHECK constraint"),
	D ("D", null, "DEFAULT (constraint or stand-alone)"),
	F ("F", SQLServerTableForeignKey.class, "FOREIGN KEY constraint"),
	FN ("FN", SQLServerProcedure.class, "SQL scalar function"),
	FS ("FS", SQLServerProcedure.class, "Assembly (CLR) scalar-function"),
	FT ("FT", SQLServerProcedure.class, "Assembly (CLR) table-valued function"),
	IF ("IF", SQLServerProcedure.class, "SQL inline table-valued function"),
	IT ("IT", SQLServerTable.class, "Internal table"),
	P ("P", SQLServerProcedure.class, "SQL Stored Procedure"),
	PC ("PC", SQLServerProcedure.class, "Assembly (CLR) stored-procedure"),
	PG ("PG", null, "Plan guide"),
	PK ("PK", SQLServerTableUniqueKey.class, "PRIMARY KEY constraint"),
	R ("R", null, "Rule (old-style, stand-alone)"),
	RF ("RF", null, "Replication-filter-procedure"),
	S ("S", SQLServerTable.class, "System base table"),
	SN ("SN", SQLServerSynonym.class, "Synonym"),
	SQ ("SQ", null, "Service queue"),
	TA ("TA", null, "Assembly (CLR) DML trigger"),
	TF ("TF", SQLServerProcedure.class, "SQL table-valued-function"),
	TR ("TR", SQLServerTableTrigger.class, "SQL DML trigger"),
	TT ("TT", null, "Table type"),
	U ("U", SQLServerTable.class, "Table (user-defined)"),
	UQ ("UQ", SQLServerTableUniqueKey.class, "UNIQUE constraint"),
	V ("V", SQLServerView.class, "View"),
	X ("X", SQLServerProcedure.class, "Extended stored procedure");


    private final String type;
    private final String description;
    private final Class<? extends DBSObject> theClass;

    private static final Log log = Log.getLog(SQLServerObjectType.class);

    SQLServerObjectType(String type, Class<? extends DBSObject> theClass, String description) {
        this.type = type;
        this.theClass = theClass;
        this.description = description;
    }

    @Override
    public String getTypeName() {
        return type;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getImage() {
        return null;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass() {
        return theClass;
    }

    @Override
    public String toString() {
        return type;
    }

    public DBSObject findObject(DBRProgressMonitor monitor, SQLServerDatabase database, SQLServerSchema schema, String objectName) throws DBException {
        if (schema == null) {
            log.debug("Null schema in table " + objectName + " search (" + name() + ")");
            return null;
        }

        if (SQLServerTableBase.class.isAssignableFrom(theClass)) {
            return schema.getChild(monitor, objectName);
        } else if (SQLServerProcedure.class.isAssignableFrom(theClass)) {
            return schema.getProcedure(monitor, objectName);
        } else {
            log.debug("Unsupported object for SQL Server search: " + name());
            return null;
        }
    }

}
