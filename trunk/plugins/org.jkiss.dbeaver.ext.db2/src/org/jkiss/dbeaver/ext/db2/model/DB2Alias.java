/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2AliasType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Alias. Can be on DB2Table, DB2Sequence or DB2Module
 * 
 * @author Denis Forveille
 */
public class DB2Alias extends DB2SchemaObject {

    private DB2AliasType type;
    private DBSObject targetObject;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2Alias(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult) throws DBException
    {
        super(schema, JDBCUtils.safeGetString(dbResult, "NAME"), true);

        this.type = CommonUtils.valueOf(DB2AliasType.class, JDBCUtils.safeGetString(dbResult, "TYPE"));
        String baseSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "BASE_SCHEMA");
        String baseObjectName = JDBCUtils.safeGetString(dbResult, "BASE_NAME");

        DB2Schema targetSchema = getDataSource().getSchema(monitor, baseSchemaName);
        switch (type) {
        case TABLE:
            this.targetObject = targetSchema.getTable(monitor, baseObjectName);
            break;

        case MODULE:
            this.targetObject = targetSchema.getModule(monitor, baseObjectName);
            break;

        case SEQUENCE:
            this.targetObject = targetSchema.getSequence(monitor, baseObjectName);
            break;
        }
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 2)
    public DB2Schema getSchema()
    {
        return super.getSchema();
    }

    @Property(viewable = true, editable = false, order = 3)
    public DB2AliasType getType()
    {
        return type;
    }

    @Property(viewable = true, editable = false, order = 4)
    public DBSObject getTargetObject()
    {
        return targetObject;
    }

}
