/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.model.access.DBAPrivilege;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;

/**
 * DB2 User or Group Authorisations
 * 
 * @author Denis Forveille
 */
public class DB2UserAuth extends DB2Object<DB2UserBase> implements DBAPrivilege {

    private DBSObject grantor;
    private DB2GrantorGranteeType grantorType;

    private DB2ObjectType objectType;
    private DB2Schema objectSchema;
    private DBSObject object;

    private DB2AuthHeldType control;
    private DB2AuthHeldType alter;
    private DB2AuthHeldType delete;
    private DB2AuthHeldType index;
    private DB2AuthHeldType insert;
    private DB2AuthHeldType reference;
    private DB2AuthHeldType select;
    private DB2AuthHeldType update;
    private DB2AuthHeldType usage;

    // -----------------------
    // Constructors
    // -----------------------
    public DB2UserAuth(DBRProgressMonitor monitor, DB2UserBase userOrGroup, ResultSet resultSet) throws DBException
    {
        super(userOrGroup, JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_SCHEMA") + "."
            + JDBCUtils.safeGetString(resultSet, "OBJ_NAME"), true);

        DB2DataSource db2DataSource = userOrGroup.getDataSource();

        String grantorName = JDBCUtils.safeGetString(resultSet, "GRANTOR");
        this.grantorType = CommonUtils.valueOf(DB2GrantorGranteeType.class, JDBCUtils.safeGetString(resultSet, "GRANTORTYPE"));
        switch (grantorType) {
        case U:
            this.grantor = userOrGroup.getDataSource().getUser(monitor, grantorName);
            break;
        case G:
            this.grantor = userOrGroup.getDataSource().getGroup(monitor, grantorName);
            break;
        default:
            break;
        }

        String objectSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_SCHEMA");
        this.objectSchema = db2DataSource.getSchema(monitor, objectSchemaName);

        String objectName = JDBCUtils.safeGetString(resultSet, "OBJ_NAME");
        this.objectType = CommonUtils.valueOf(DB2ObjectType.class, JDBCUtils.safeGetString(resultSet, "OBJ_TYPE"));
        switch (objectType) {
        case TABLE:
            // May be Table or View..
            this.object = DB2Utils.findTableBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            if (this.object == null) {
                this.objectType = DB2ObjectType.VIEW;
                this.object = DB2Utils.findViewBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            }

            this.control = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "CONTROLAUTH"));
            this.alter = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "ALTERAUTH"));
            this.delete = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "DELETEAUTH"));
            this.index = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "INDEXAUTH"));
            this.insert = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "INSERTAUTH"));
            this.reference = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "REFAUTH"));
            this.select = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "SELECTAUTH"));
            this.update = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "UPDATEAUTH"));
            break;

        case INDEX:
            this.object = DB2Utils.findIndexBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);

            this.control = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "CONTROLAUTH"));
            break;

        case SEQUENCE:
            this.object = DB2Utils.findSequenceBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);

            this.alter = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "ALTERAUTH"));
            this.usage = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "USAGEAUTH"));
            break;

        case TABLESPACE:
            this.object = db2DataSource.getTablespace(monitor, objectName);

            this.usage = CommonUtils.valueOf(DB2AuthHeldType.class, JDBCUtils.safeGetString(resultSet, "USAGEAUTH"));
            break;

        default:
            break;
        }

    }

    // -----------------
    // Properties
    // -----------------

    @Override
    @Property(hidden = true)
    public String getName()
    {
        return super.getName();
    }

    @Property(viewable = true, order = 1)
    public DB2Schema getObjectSchema()
    {
        return objectSchema;
    }

    @Property(viewable = true, order = 2)
    public DBSObject getObject()
    {
        return object;
    }

    @Property(viewable = true, order = 3)
    public DB2ObjectType getObjectType()
    {
        return objectType;
    }

    @Property(viewable = true, order = 7)
    public DBSObject getGrantor()
    {
        return grantor;
    }

    @Property(viewable = true, order = 8)
    public DB2GrantorGranteeType getGrantorType()
    {
        return grantorType;
    }

    @Property(viewable = true, order = 20, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getControl()
    {
        return control;
    }

    @Property(viewable = true, order = 21, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getAlter()
    {
        return alter;
    }

    @Property(viewable = true, order = 22, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getDelete()
    {
        return delete;
    }

    @Property(viewable = true, order = 23, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getIndex()
    {
        return index;
    }

    @Property(viewable = true, order = 24, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getInsert()
    {
        return insert;
    }

    @Property(viewable = true, order = 25, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getReference()
    {
        return reference;
    }

    @Property(viewable = true, order = 26, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getSelect()
    {
        return select;
    }

    @Property(viewable = true, order = 27, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getUpdate()
    {
        return update;
    }

    @Property(viewable = true, order = 28, category = DB2Constants.CAT_AUTH)
    public DB2AuthHeldType getUsage()
    {
        return usage;
    }

}
