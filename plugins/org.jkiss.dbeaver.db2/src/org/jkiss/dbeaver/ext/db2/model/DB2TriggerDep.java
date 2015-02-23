/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.db2.model;

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
