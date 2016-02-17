/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableDepType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 Views like Dependency
 * 
 * @author Denis Forveille
 */
public class DB2ViewBaseDep extends DB2Object<DB2ViewBase> {

    private DB2TableDepType tableDepType;
    private DB2Schema depSchema;
    private String depModuleId;
    private String tabAuth;

    private DBSObject depObject;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2ViewBaseDep(DBRProgressMonitor monitor, DB2ViewBase db2ViewBase, ResultSet resultSet) throws DBException
    {
        // TODO DF: Bad should be BTYPE+BSCHEMA+BNAME
        super(db2ViewBase, JDBCUtils.safeGetString(resultSet, "BNAME"), true);

        DB2DataSource db2DataSource = db2ViewBase.getDataSource();

        this.tabAuth = JDBCUtils.safeGetString(resultSet, "TABAUTH");
        this.tableDepType = CommonUtils.valueOf(DB2TableDepType.class, JDBCUtils.safeGetString(resultSet, "BTYPE"));

        String depSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "BSCHEMA");

        if (db2DataSource.isAtLeastV9_5()) {
            this.depModuleId = JDBCUtils.safeGetString(resultSet, "BMODULEID");
        }

        DB2ObjectType db2ObjectType = tableDepType.getDb2ObjectType();
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
    @Property(viewable = true, editable = false, id = "Name", order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, editable = false, order = 2)
    public DB2TableDepType getTableDepType()
    {
        return tableDepType;
    }

    @Property(viewable = true, editable = false, order = 3)
    public DB2Schema getDepSchema()
    {
        return depSchema;
    }

    @Property(viewable = true, editable = false, order = 4)
    public DBSObject getDepObject()
    {
        return depObject;
    }

    @Property(viewable = true, editable = false)
    public String getDepModuleId()
    {
        return depModuleId;
    }

    @Property(viewable = true, editable = false)
    public String getTabAuth()
    {
        return tabAuth;
    }

}
