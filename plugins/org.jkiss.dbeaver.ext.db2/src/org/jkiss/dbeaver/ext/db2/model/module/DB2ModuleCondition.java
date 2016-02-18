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
package org.jkiss.dbeaver.ext.db2.model.module;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.model.DB2Object;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2OwnerType;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.Timestamp;

/**
 * DB2 Module Condition
 * 
 * @author Denis Forveille
 */
public class DB2ModuleCondition extends DB2Object<DB2Module> {

    private Integer id;
    private String owner;
    private DB2OwnerType ownerType;
    private String sqlState;
    private Timestamp createTime;
    private String remarks;

    // -----------------------
    // Constructors
    // -----------------------

    public DB2ModuleCondition(DB2Module db2Module, ResultSet dbResult) throws DBException
    {

        super(db2Module, JDBCUtils.safeGetString(dbResult, "CONDNAME"), true);

        this.id = JDBCUtils.safeGetInteger(dbResult, "CONDID");
        this.owner = JDBCUtils.safeGetString(dbResult, "OWNER");
        this.ownerType = CommonUtils.valueOf(DB2OwnerType.class, JDBCUtils.safeGetString(dbResult, "OWNERTYPE"));
        this.sqlState = JDBCUtils.safeGetString(dbResult, "SQLSTATE");
        this.createTime = JDBCUtils.safeGetTimestamp(dbResult, "CREATE_TIME");
        this.remarks = JDBCUtils.safeGetString(dbResult, "REMARKS");
    }

    @Nullable
    @Override
    public String getDescription()
    {
        return remarks;
    }

    // -----------------------
    // Properties
    // -----------------------

    @NotNull
    @Override
    @Property(viewable = true, order = 1)
    public String getName()
    {
        return name;
    }

    @Property(viewable = true, order = 2)
    public Integer getId()
    {
        return id;
    }

    @Property(viewable = true, order = 3, category = DB2Constants.CAT_OWNER)
    public String getOwner()
    {
        return owner;
    }

    @Property(viewable = true, order = 4, category = DB2Constants.CAT_OWNER)
    public DB2OwnerType getOwnerType()
    {
        return ownerType;
    }

    @Property(viewable = true, order = 5)
    public String getSqlState()
    {
        return sqlState;
    }

    @Property(viewable = false, order = 7, category = DB2Constants.CAT_DATETIME)
    public Timestamp getCreateTime()
    {
        return createTime;
    }

}
