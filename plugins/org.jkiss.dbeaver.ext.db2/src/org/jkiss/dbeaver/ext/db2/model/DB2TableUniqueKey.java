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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2ConstraintCheckData;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2YesNo;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.util.List;

/**
 * DB2 Table Unique Key
 * 
 * @author Denis Forveille
 */
public class DB2TableUniqueKey extends JDBCTableConstraint<DB2Table> {

    private String owner;
    private DB2OwnerType ownerType;
    private Boolean enforced;
    private Boolean trusted;
    private DB2ConstraintCheckData checkExistingData;
    private Boolean enableQueryOpt;
    private String remarks;

    private List<DB2TableKeyColumn> columns;

    // -----------------
    // Constructor
    // -----------------

    public DB2TableUniqueKey(DBRProgressMonitor monitor, DB2Table table, ResultSet dbResult, DBSEntityConstraintType type)
        throws DBException
    {
        super(table, JDBCUtils.safeGetString(dbResult, "CONSTNAME"), null, type, true);

        DB2DataSource db2DataSource = table.getDataSource();

        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.enforced = JDBCUtils.safeGetBoolean(dbResult, "ENFORCED", DB2YesNo.Y.name());
        this.checkExistingData = CommonUtils.valueOf(DB2ConstraintCheckData.class,
            JDBCUtils.safeGetString(dbResult, "CHECKEXISTINGDATA"));
        this.enableQueryOpt = JDBCUtils.safeGetBoolean(dbResult, "ENABLEQUERYOPT", DB2YesNo.Y.name());
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");

        if (db2DataSource.isAtLeastV9_5()) {
            this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        }
        if (db2DataSource.isAtLeastV10_1()) {
            this.trusted = JDBCUtils.safeGetBoolean(dbResult, "TRUSTED", DB2YesNo.Y.name());
        }

    }

    public DB2TableUniqueKey(DB2Table db2Table, DBSEntityConstraintType constraintType)
    {
        super(db2Table, null, null, constraintType, false);
        this.ownerType = DB2OwnerType.U;
    }

    // -----------------
    // Business Contract
    // -----------------

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(), getTable().getContainer(), getTable(), this);
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    // -----------------
    // Columns
    // -----------------

    @Override
    public List<? extends DBSEntityAttributeRef> getAttributeReferences(DBRProgressMonitor monitor) throws DBException
    {
        return columns;
    }

    public void setColumns(List<DB2TableKeyColumn> columns)
    {
        this.columns = columns;
    }

    // -----------------
    // Properties
    // -----------------
    @Override
    @Property(viewable = true, editable = false, order = 2)
    public DB2Table getTable()
    {
        return super.getTable();
    }

    @NotNull
    @Override
    @Property(viewable = true, editable = false, order = 3)
    public DBSEntityConstraintType getConstraintType()
    {
        return super.getConstraintType();
    }

    @Nullable
    @Override
    @Property(viewable = true, editable = false, order = 4)
    public String getDescription()
    {
        return remarks;
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

    @Property(viewable = false, editable = false)
    public Boolean getEnforced()
    {
        return enforced;
    }

    @Property(viewable = false, editable = false)
    public Boolean getTrusted()
    {
        return trusted;
    }

    @Property(viewable = false, editable = false)
    public DB2ConstraintCheckData getCheckExistingData()
    {
        return checkExistingData;
    }

    @Property(viewable = false, editable = false)
    public Boolean getEnableQueryOpt()
    {
        return enableQueryOpt;
    }

}
