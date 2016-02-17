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
import org.jkiss.dbeaver.ext.db2.model.dict.DB2XMLSchemaDepType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 XML Schema Dependency
 * 
 * @author Denis Forveille
 */
public class DB2XMLSchemaDep extends DB2Object<DB2XMLSchema> {

    private DB2XMLSchemaDepType xmlSchemaDepType;
    private DB2Schema depSchema;
    private String depModuleName;
    private String depModuleId;
    private String tabAuth;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2XMLSchemaDep(DB2XMLSchema db2XMLSchema, ResultSet resultSet) throws DBException
    {
        // TODO DF: Bad should be BTYPE+BSCHEMA+BNAME
        super(db2XMLSchema, JDBCUtils.safeGetString(resultSet, "BNAME"), true);

        this.depModuleName = JDBCUtils.safeGetString(resultSet, "BMODULENAME");
        this.depModuleId = JDBCUtils.safeGetString(resultSet, "BMODULEID");
        this.tabAuth = JDBCUtils.safeGetString(resultSet, "TABAUTH");

        String depSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "BSCHEMA");

        String depType = JDBCUtils.safeGetString(resultSet, "BTYPE");
        this.xmlSchemaDepType = CommonUtils.valueOf(DB2XMLSchemaDepType.class, depType);

        if (this.xmlSchemaDepType != null) {
            DB2ObjectType db2ObjectType = xmlSchemaDepType.getDb2ObjectType();
            if (db2ObjectType != null) {
                depSchema = getDataSource().getSchemaCache().getCachedObject(depSchemaName);
            }
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
    public DB2XMLSchemaDepType getXmlSchemaDepType()
    {
        return xmlSchemaDepType;
    }

    @Property(viewable = true, editable = false, order = 3)
    public DB2Schema getDepSchema()
    {
        return depSchema;
    }

    @Property(viewable = true, editable = false, order = 4)
    public DBSObject getDepObject(DBRProgressMonitor monitor) throws DBException
    {
        if (xmlSchemaDepType == null || xmlSchemaDepType.getDb2ObjectType() == null) {
            return null;
        }
        // Some dependncies are in Modules...Concatenate modulename and name
        String name = getName();
        if (depModuleName != null) {
            name = depModuleName + "." + name;
        }
        return xmlSchemaDepType.getDb2ObjectType().findObject(monitor, depSchema, name);
    }

    @Property(viewable = false, editable = false)
    public String getDepModuleId()
    {
        return depModuleId;
    }

    @Property(viewable = false, editable = false)
    public String getTabAuth()
    {
        return tabAuth;
    }

}
