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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.editors.DB2StatefulObject;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTable;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Collections;

/**
 * Super class for DB2 Tables and Views
 * 
 * @author Denis Forveille
 */
public abstract class DB2TableBase extends JDBCTable<DB2DataSource, DB2Schema> implements DBPNamedObject2, DBPRefreshableObject,
    DB2StatefulObject {

    private String owner;
    private DB2OwnerType ownerType;

    private Integer tableId;

    private Timestamp createTime;
    private Timestamp alterTime;
    private Timestamp invalidateTime;
    private Timestamp lastRegenTime;

    private String remarks;

    // -----------------
    // Constructors
    // -----------------
    public DB2TableBase(DBRProgressMonitor monitor, DB2Schema schema, ResultSet dbResult)
    {
        super(schema, true);

        setName(JDBCUtils.safeGetString(dbResult, "TABNAME"));

        DB2DataSource db2DataSource = (DB2DataSource) schema.getDataSource();

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.tableId = JDBCUtils.safeGetInteger(dbResult, "TABLEID");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.invalidateTime = JDBCUtils.safeGetTimestamp(dbResult, "INVALIDATE_TIME");
        this.lastRegenTime = JDBCUtils.safeGetTimestamp(dbResult, "LAST_REGEN_TIME");

        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.alterTime = JDBCUtils.safeGetTimestamp(dbResult, "ALTER_TIME");
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        }
    }

    public DB2TableBase(DB2Schema container, String name, Boolean persisted)
    {
        super(container, name, persisted);
    }

    // -----------------
    // Business Contract
    // -----------------

    @Override
    public void refreshObjectState(DBRProgressMonitor monitor) throws DBCException
    {
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getSchema(), this);
    }

    // -----------------
    // Associations (Imposed from DBSTable). In DB2, Most of objects "derived"
    // from Tables don't have those..
    // -----------------

    @Override
    public Collection<DB2Index> getIndexes(DBRProgressMonitor monitor) throws DBException
    {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public Collection<DB2TableUniqueKey> getConstraints(DBRProgressMonitor monitor) throws DBException
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<DB2TableForeignKey> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        return Collections.emptyList();
    }

    @Override
    public Collection<DB2TableReference> getReferences(DBRProgressMonitor monitor) throws DBException
    {
        return Collections.emptyList();
    }

    // -----------------
    // Properties
    // -----------------

    @NotNull
    @Override
    @Property(viewable = true, editable = false, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    public String getName()
    {
        return super.getName();
    }

    @Override
    @Property(viewable = true, editable = false, order = 2)
    public DB2Schema getSchema()
    {
        return super.getContainer();
    }

    @Property(viewable = false, editable = false, order = 100, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

    @Property(viewable = false, editable = false, order = 101, category = DB2Constants.CAT_DATETIME)
    public Timestamp getAlterTime()
    {
        return alterTime;
    }

    @Property(viewable = false, editable = false, order = 102, category = DB2Constants.CAT_DATETIME)
    public Timestamp getInvalidateTime()
    {
        return invalidateTime;
    }

    @Property(viewable = false, editable = false, order = 103, category = DB2Constants.CAT_DATETIME)
    public Timestamp getLastRegenTime()
    {
        return lastRegenTime;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = true, order = 98)
    public Integer getTableId()
    {
        return tableId;
    }

    @Nullable
    @Override
    @Property(viewable = false, order = 99, editable = true, updatable = true)
    public String getDescription()
    {
        return remarks;
    }

    public void setDescription(String description)
    {
        this.remarks = description;
    }

}
