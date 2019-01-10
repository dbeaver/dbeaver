/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2AliasType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAlias;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Alias. Can be on DB2Table, DB2Sequence or DB2Module
 * 
 * @author Denis Forveille
 */
public class DB2Alias extends DB2SchemaObject implements DBSAlias {

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

    @Override
    public DBSObject getTargetObject(DBRProgressMonitor monitor) throws DBException {
        return getTargetObject();
    }

}
