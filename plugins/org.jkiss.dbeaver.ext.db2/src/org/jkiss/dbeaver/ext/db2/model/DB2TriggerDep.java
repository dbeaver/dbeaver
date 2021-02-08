/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TriggerDepType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Trigger Dependency
 * 
 * @author Denis Forveille
 */
public class DB2TriggerDep extends DB2Object<DB2Trigger> {

    private DB2TriggerDepType triggerDepType;
    private DB2Schema depSchema;
    private String depModuleId;
    private String tabAuth;

    private DBSObject depObject;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2TriggerDep(DBRProgressMonitor monitor, DB2Trigger db2Trigger, ResultSet resultSet) throws DBException
    {
        // TODO DF: Bad should be BTYPE+BSCHEMA+BNAME
        super(db2Trigger, JDBCUtils.safeGetString(resultSet, "BNAME"), true);

        this.depModuleId = JDBCUtils.safeGetString(resultSet, "BMODULEID");
        this.tabAuth = JDBCUtils.safeGetString(resultSet, "TABAUTH");
        this.triggerDepType = CommonUtils.valueOf(DB2TriggerDepType.class, JDBCUtils.safeGetString(resultSet, "BTYPE"));

        String depSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "BSCHEMA");

        DB2ObjectType db2ObjectType = triggerDepType.getDb2ObjectType();
        if (db2ObjectType != null) {
            depSchema = getDataSource().getSchema(monitor, depSchemaName);
            depObject = db2ObjectType.findObject(monitor, depSchema, getName());
        }
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, id = "Name", order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 2)
    public DB2TriggerDepType getTriggerDepType()
    {
        return triggerDepType;
    }

    @Property(viewable = true, order = 3)
    public DB2Schema getDepSchema()
    {
        return depSchema;
    }

    @Property(viewable = true, order = 4)
    public DBSObject getDepObject()
    {
        return depObject;
    }

    @Property(viewable = true)
    public String getDepModuleId()
    {
        return depModuleId;
    }

    @Property(viewable = true)
    public String getTabAuth()
    {
        return tabAuth;
    }

}
