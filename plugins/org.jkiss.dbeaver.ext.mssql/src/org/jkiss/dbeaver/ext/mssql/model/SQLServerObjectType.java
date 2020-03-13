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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;

import java.util.ArrayList;
import java.util.List;

/**
 * Object type
 */
public enum SQLServerObjectType implements DBSObjectType {

	AF ("AF", null, DBIcon.TREE_FUNCTION, "Aggregate function (CLR)"),
	C ("C", SQLServerTableCheckConstraint.class, DBIcon.TREE_CONSTRAINT, "CHECK constraint"),
	D ("D", null, DBIcon.TREE_CONSTRAINT, "DEFAULT (constraint or stand-alone)"),
	F ("F", SQLServerTableForeignKey.class, DBIcon.TREE_CONSTRAINT, "FOREIGN KEY constraint"),
	FN ("FN", SQLServerProcedure.class, DBIcon.TREE_FUNCTION, "SQL scalar function"),
	FS ("FS", SQLServerProcedure.class, DBIcon.TREE_FUNCTION, "Assembly (CLR) scalar-function"),
	FT ("FT", SQLServerProcedure.class, DBIcon.TREE_FUNCTION, "Assembly (CLR) table-valued function"),
	IF ("IF", SQLServerProcedure.class, DBIcon.TREE_FUNCTION, "SQL inline table-valued function"),
	IT ("IT", SQLServerTable.class, DBIcon.TREE_TABLE_SYSTEM, "Internal table"),
	P ("P", SQLServerProcedure.class, DBIcon.TREE_PROCEDURE, "SQL Stored Procedure"),
	PC ("PC", SQLServerProcedure.class, DBIcon.TREE_PROCEDURE, "Assembly (CLR) stored-procedure"),
	PG ("PG", null, null, "Plan guide"),
	PK ("PK", SQLServerTableUniqueKey.class, DBIcon.TREE_UNIQUE_KEY, "PRIMARY KEY constraint"),
	R ("R", null, null, "Rule (old-style, stand-alone)"),
	RF ("RF", null, DBIcon.TREE_PROCEDURE, "Replication-filter-procedure"),
	S ("S", SQLServerTable.class, DBIcon.TREE_TABLE, "System base table"),
	SN ("SN", SQLServerSynonym.class, DBIcon.TREE_SYNONYM, "Synonym"),
	SQ ("SQ", null, null, "Service queue"),
	TA ("TA", null, DBIcon.TREE_TRIGGER, "Assembly (CLR) DML trigger"),
	TF ("TF", SQLServerProcedure.class, DBIcon.TREE_FUNCTION, "SQL table-valued-function"),
	TR ("TR", SQLServerTableTrigger.class, DBIcon.TREE_TRIGGER, "SQL DML trigger"),
	TT ("TT", null, DBIcon.TREE_DATA_TYPE, "Table type"),
	U ("U", SQLServerTable.class, DBIcon.TREE_TABLE, "Table"),
	UQ ("UQ", SQLServerTableUniqueKey.class, DBIcon.TREE_CONSTRAINT, "UNIQUE constraint"),
	V ("V", SQLServerView.class, DBIcon.TREE_VIEW, "View"),
	X ("X", SQLServerProcedure.class, DBIcon.TREE_PROCEDURE, "Extended stored procedure");


    private final String type;
    private final String description;
    private final Class<? extends DBSObject> theClass;
    private final DBPImage icon;

    private static final Log log = Log.getLog(SQLServerObjectType.class);

    SQLServerObjectType(String type, Class<? extends DBSObject> theClass, DBPImage icon, String description) {
        this.type = type;
        this.theClass = theClass;
        this.icon = icon;
        this.description = description;
    }

    @Override
    public String getTypeName() {
        return description;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBPImage getImage() {
        return icon;
    }

    @Override
    public Class<? extends DBSObject> getTypeClass() {
        return theClass;
    }

    public String getTypeID() {
        return type;
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
        } else if (SQLServerSynonym.class.isAssignableFrom(theClass)) {
            SQLServerSynonym synonym = schema.getSynonym(monitor, objectName);
            return synonym;
        } else {
            log.debug("Unsupported object for SQL Server search: " + name());
            return null;
        }
    }

    public static List<SQLServerObjectType> getTypesForClass(Class<?> theClass) {
        List<SQLServerObjectType> result = new ArrayList<>();
        for (SQLServerObjectType ot : SQLServerObjectType.values()) {
            if (ot.theClass == theClass) {
                result.add(ot);
            }
        }
        return result;
    }
}
